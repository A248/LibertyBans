/* 
 * LibertyBans-env-velocity
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-velocity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-velocity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-velocity. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.velocity;

import java.net.InetAddress;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import net.kyori.adventure.text.Component;
import space.arim.libertybans.core.punish.Guardian;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

@Singleton
public class ConnectionListener extends VelocityParallelisedListener<LoginEvent, Component> {

	private final Guardian guardian;

	@Inject
	public ConnectionListener(PluginContainer plugin, ProxyServer server, Guardian guardian) {
		super(plugin, server);
		this.guardian = guardian;
	}

	@Override
	public Class<LoginEvent> getEventClass() {
		return LoginEvent.class;
	}

	@Override
	protected CentralisedFuture<Component> beginComputation(LoginEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		String name = player.getUsername();
		InetAddress address = player.getRemoteAddress().getAddress();
		return guardian.executeAndCheckConnection(uuid, name, address);
	}

	@Override
	protected void executeNonNullResult(LoginEvent event, Component message) {
		event.setResult(ComponentResult.denied(message));
	}

}
