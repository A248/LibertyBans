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
package space.arim.libertybans.spigot;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import space.arim.libertybans.api.Punishment;

public class ConnectionListener implements Listener {

	private final SpigotEnv env;
	
	public ConnectionListener(SpigotEnv env) {
		this.env = env;
	}

	@EventHandler
	public void onConnect(AsyncPlayerPreLoginEvent evt) {
		UUID uuid = evt.getUniqueId();
		String name = evt.getName();
		byte[] address = evt.getAddress().getAddress();
		Punishment punishment = env.core.getSelector().executeAndCheckConnection(uuid, name, address).join();
		if (punishment == null) {
			return;
		}
		String message = env.core.getConfigs().getPunishmentMessage(punishment);
		evt.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
	}
	
}
