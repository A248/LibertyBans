/* 
 * LibertyBans-env-spigot
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-spigot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-spigot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-spigot. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.spigot;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.api.Punishment;

public class ConnectionListener implements Listener {

	private final SpigotEnv env;
	
	private final ConcurrentMap<UUID, CentralisedFuture<Punishment>> apples = new ConcurrentHashMap<>();
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	public ConnectionListener(SpigotEnv env) {
		this.env = env;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onConnectLow(AsyncPlayerPreLoginEvent evt) {
		if (evt.getLoginResult() != Result.ALLOWED) {
			logger.debug("Player '{}' is already blocked by the server or another plugin", evt.getName());
			return;
		}
		UUID uuid = evt.getUniqueId();
		String name = evt.getName();
		byte[] address = evt.getAddress().getAddress();
		CentralisedFuture<Punishment> previous = apples.put(uuid,
				env.core.getSelector().executeAndCheckConnection(uuid, name, address));
		assert previous == null : previous.join();
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onConnectHigh(AsyncPlayerPreLoginEvent evt) {
		CentralisedFuture<Punishment> future = apples.remove(evt.getUniqueId());
		if (future == null) {
			if (evt.getLoginResult() == Result.ALLOWED) {
				logger.error(
						"Critical: Player ({}, {}, {}) was previously blocked by the server or another plugin, "
						+ "but since then, some plugin has *uncancelled* the blocking. "
						+ "This may lead to bans not being checked and enforced.",
						evt.getUniqueId(), evt.getName(), evt.getAddress());
			}
			return;
		}
		Punishment punishment = future.join();
		if (punishment == null) {
			logger.trace("Letting '{}' through the gates", evt.getName());
			return;
		}
		CentralisedFuture<SendableMessage> message = env.core.getFormatter().getPunishmentMessage(punishment);
		assert message.isDone() : punishment;

		evt.disallow(Result.KICK_BANNED, message.join().toLegacyMessageString(ChatColor.COLOR_CHAR));
	}
	
}
