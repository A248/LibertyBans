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

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.chat.AdventureTextConverter;

import space.arim.libertybans.core.punish.Enforcer;

import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

@Singleton
public class ConnectionListener extends VelocityParallelisedListener<LoginEvent, SendableMessage> {
	
	private final Enforcer enforcer;
	private final AdventureTextConverter textConverter;
	
	@Inject
	public ConnectionListener(PluginContainer plugin, ProxyServer server, Enforcer enforcer) {
		this(plugin, server, enforcer, new AdventureTextConverter());
	}

	ConnectionListener(PluginContainer plugin, ProxyServer server, Enforcer enforcer, AdventureTextConverter textConverter) {
		super(plugin, server);
		this.enforcer = enforcer;
		this.textConverter = textConverter;
	}

	@Override
	public Class<LoginEvent> getEventClass() {
		return LoginEvent.class;
	}

	@Override
	protected CentralisedFuture<SendableMessage> beginComputation(LoginEvent event) {
		Player player = event.getPlayer();
		UUID uuid = player.getUniqueId();
		String name = player.getUsername();
		InetAddress address = player.getRemoteAddress().getAddress();
		return enforcer.executeAndCheckConnection(uuid, name, address);
	}

	@Override
	protected void executeNonNullResult(LoginEvent event, SendableMessage message) {
		event.setResult(ComponentResult.denied(textConverter.convert(message)));
	}

}
