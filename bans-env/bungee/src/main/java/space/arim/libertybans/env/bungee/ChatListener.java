/* 
 * LibertyBans-env-bungee
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-bungee is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-bungee is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-bungee. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.bungee;

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.core.env.ParallelisedListener;

import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class ChatListener extends ParallelisedListener<ChatEvent, SendableMessage> implements Listener {

	private final BungeeEnv env;
	
	ChatListener(BungeeEnv env) {
		this.env = env;
	}
	
	@Override
	public void register() {
		env.getPlugin().getProxy().getPluginManager().registerListener(env.getPlugin(), this);
	}
	
	@Override
	public void unregister() {
		env.getPlugin().getProxy().getPluginManager().unregisterListener(this);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onChatLow(ChatEvent evt) {
		Connection sender = evt.getSender();
		if (!(sender instanceof ProxiedPlayer)) {
			return;
		}
		ProxiedPlayer player = (ProxiedPlayer) sender;
		byte[] address = env.getEnforcer().getAddress(player).getAddress();
		String command = (evt.isCommand()) ? evt.getMessage().substring(1) : evt.getMessage();
		begin(evt, env.core.getEnforcer().checkChat(player.getUniqueId(), address, command));
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onChatHigh(ChatEvent evt) {
		SendableMessage message = withdraw(evt);
		if (message == null) {
			return;
		}
		evt.setCancelled(true);
		env.handle.sendMessage((ProxiedPlayer) evt.getSender(), message);
	}
	
}
