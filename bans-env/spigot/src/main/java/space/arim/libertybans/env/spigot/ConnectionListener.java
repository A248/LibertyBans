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

import java.net.InetAddress;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import space.arim.api.chat.SendableMessage;
import space.arim.api.chat.serialiser.LegacyCodeSerialiser;

import space.arim.libertybans.core.punish.Enforcer;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class ConnectionListener extends SpigotParallelisedListener<AsyncPlayerPreLoginEvent, SendableMessage> {
	
	private final Enforcer enforcer;
	
	@Inject
	public ConnectionListener(JavaPlugin plugin, Enforcer enforcer) {
		super(plugin);
		this.enforcer = enforcer;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onConnectLow(AsyncPlayerPreLoginEvent event) {
		if (event.getLoginResult() != Result.ALLOWED) {
			debugPrematurelyDenied(event);
			return;
		}
		UUID uuid = event.getUniqueId();
		String name = event.getName();
		InetAddress address = event.getAddress();
		begin(event, enforcer.executeAndCheckConnection(uuid, name, address));
	}
	
	@Override
	protected boolean isAllowed(AsyncPlayerPreLoginEvent event) {
		return event.getLoginResult() == Result.ALLOWED;
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onConnectHigh(AsyncPlayerPreLoginEvent event) {
		SendableMessage message = withdraw(event);
		if (message == null) {
			debugResultPermitted(event);
			return;
		}
		event.disallow(Result.KICK_BANNED, LegacyCodeSerialiser.getInstance(ChatColor.COLOR_CHAR).serialise(message));
	}
	
}
