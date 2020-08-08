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

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.chat.AdventureTextConverter;

import space.arim.libertybans.api.Punishment;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;

public class ConnectionListener {

	private final VelocityEnv env;
	
	final Map<LoginEvent, CentralisedFuture<Punishment>> logins;
	
	private final EventHandler<LoginEvent> earlyHandler = new EarlyHandler();
	private final EventHandler<LoginEvent> lateHandler = new LateHandler();
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	ConnectionListener(VelocityEnv env) {
		this.env = env;
		logins = Collections.synchronizedMap(new IdentityHashMap<>());
	}
	
	void register() {
		PluginContainer plugin = env.getPlugin();
		EventManager evtManager = env.getServer().getEventManager();
		evtManager.register(plugin, LoginEvent.class, PostOrder.EARLY, earlyHandler);
		evtManager.register(plugin, LoginEvent.class, PostOrder.LATE, lateHandler);
	}
	
	void unregister() {
		PluginContainer plugin = env.getPlugin();
		EventManager evtManager = env.getServer().getEventManager();
		evtManager.unregister(plugin, earlyHandler);
		evtManager.unregister(plugin, lateHandler);
	}
	
	private class EarlyHandler implements EventHandler<LoginEvent> {

		@Override
		public void execute(LoginEvent evt) {
			if (!evt.getResult().isAllowed()) {
				logger.debug("Player '{}' is already blocked by the server or another plugin", evt.getPlayer().getUsername());
				return;
			}
			Player player = evt.getPlayer();
			UUID uuid = player.getUniqueId();
			String name = player.getUsername();
			byte[] address = player.getRemoteAddress().getAddress().getAddress();
			logins.put(evt, env.core.getSelector().executeAndCheckConnection(uuid, name, address));
		}
		
	}
	
	private class LateHandler implements EventHandler<LoginEvent> {

		@Override
		public void execute(LoginEvent evt) {
			CentralisedFuture<Punishment> future = logins.remove(evt);
			if (future == null) {
				if (evt.getResult().isAllowed()) {
					Player player = evt.getPlayer();
					logger.error(
							"Critical: Player ({}, {}, {}) was previously blocked by the server or another plugin, "
							+ "but since then, some plugin has *uncancelled* the blocking. "
							+ "This may lead to bans not being checked and enforced.",
							player.getUniqueId(), player.getUsername(), player.getRemoteAddress().getAddress());
				}
				return;
			}
			Player player = evt.getPlayer();
			Punishment punishment = future.join();
			if (punishment == null) {
				logger.trace("Letting '{}' through the gates", player.getUsername());
				return;
			}
			CentralisedFuture<SendableMessage> message = env.core.getFormatter().getPunishmentMessage(punishment);
			assert message.isDone() : punishment;

			evt.setResult(ComponentResult.denied(new AdventureTextConverter().convertFrom(message.join())));
		}
		
	}
	
}
