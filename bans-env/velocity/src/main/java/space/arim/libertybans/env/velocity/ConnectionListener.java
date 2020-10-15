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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.chat.AdventureTextConverter;

import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;

class ConnectionListener extends VelocityParallelisedListener<LoginEvent, SendableMessage> {
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	ConnectionListener(VelocityEnv env) {
		super(env);
	}
	
	@Override
	public void register() {
		register(LoginEvent.class);
	}
	
	@Override
	protected CentralisedFuture<SendableMessage> beginFor(LoginEvent evt) {
		if (!evt.getResult().isAllowed()) {
			logger.debug("Player '{}' is already blocked by the server or another plugin", evt.getPlayer().getUsername());
			return null;
		}
		Player player = evt.getPlayer();
		UUID uuid = player.getUniqueId();
		String name = player.getUsername();
		InetAddress address = player.getRemoteAddress().getAddress();
		return env.core.getEnforcementCenter().executeAndCheckConnection(uuid, name, address);
	}
	
	@Override
	protected void absentFutureHandler(LoginEvent evt) {
		Player player = evt.getPlayer();
		if (evt.getResult().isAllowed()) {
			logger.error(
					"Critical: Player ({}, {}, {}) was previously blocked by the server or another plugin, "
					+ "but since then, some plugin has *uncancelled* the blocking. "
					+ "This may lead to bans not being checked and enforced.",
					player.getUniqueId(), player.getUsername(), player.getRemoteAddress().getAddress());
		} else {
			logger.trace("Confirmation: Player '{}' is already blocked by the server or another plugin", player.getUsername());
		}
	}
	
	@Override
	protected void withdrawFor(LoginEvent evt) {
		SendableMessage message = withdraw(evt);
		if (message == null) {
			logger.trace("Letting '{}' through the gates", evt.getPlayer().getUsername());
			return;
		}
		evt.setResult(ComponentResult.denied(new AdventureTextConverter().convert(message)));
	}
	
}
