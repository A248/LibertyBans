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

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.core.config.SyncEnforcement;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEvent;

public class ChatListener extends SpigotParallelisedListener<PlayerEvent, SendableMessage> {

	ChatListener(SpigotEnv env) {
		super(env);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onChatLow(AsyncPlayerChatEvent evt) {
		combinedBegin(evt, null);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onCommandLow(PlayerCommandPreprocessEvent evt) {
		String command = evt.getMessage();
		combinedBegin(evt, (command.charAt(0) == '/') ? command.substring(1) : command);
	}
	
	private void combinedBegin(PlayerEvent evt, String command) {
		Player player = evt.getPlayer();
		byte[] address = player.getAddress().getAddress().getAddress();
		begin(evt, env.core.getEnforcer().checkChat(player.getUniqueId(), address, command));
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onChatHigh(AsyncPlayerChatEvent evt) {
		combinedWithdraw(evt);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onCommandHigh(PlayerCommandPreprocessEvent evt) {
		combinedWithdraw(evt);
	}
	
	private void combinedWithdraw(PlayerEvent evt) {
		CentralisedFuture<SendableMessage> futureMessage = withdrawRaw(evt);
		SendableMessage message;
		if (futureMessage.isDone() || evt.isAsynchronous()) {
			message = futureMessage.join();
		} else {
			SyncEnforcement strategy = env.core.getConfigs().getConfig().getObject(
					"enforcement.sync-events-strategy", SyncEnforcement.class);
			switch (strategy) {
			case WAIT:
				message = futureMessage.join();
				break;
			case ALLOW:
				return;
			case DENY:
				message = env.core.getFormatter().parseMessage(env.core.getConfigs().getMessages().getString("misc.sync-denial-message"));
				break;
			default:
				throw new IllegalStateException("Unknown SyncEnforcement strategy " + strategy);
			}
		}
		if (message == null) {
			return;
		}
		((Cancellable) evt).setCancelled(true);
		env.handle.sendMessage(evt.getPlayer(), message);
	}
	
}
