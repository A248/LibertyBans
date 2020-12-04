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

import java.net.InetAddress;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.PlatformHandle;

import space.arim.libertybans.core.env.ParallelisedListener;
import space.arim.libertybans.core.punish.Enforcer;

import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

@Singleton
public class ChatListener extends ParallelisedListener<ChatEvent, SendableMessage> implements Listener {

	private final Plugin plugin;
	private final Enforcer enforcer;
	private final AddressReporter addressReporter;
	private final PlatformHandle handle;
	
	@Inject
	public ChatListener(Plugin plugin, Enforcer enforcer, AddressReporter addressReporter, PlatformHandle handle) {
		this.plugin = plugin;
		this.enforcer = enforcer;
		this.addressReporter = addressReporter;
		this.handle = handle;
	}
	
	@Override
	public void register() {
		plugin.getProxy().getPluginManager().registerListener(plugin, this);
	}
	
	@Override
	public void unregister() {
		plugin.getProxy().getPluginManager().unregisterListener(this);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onChatLow(ChatEvent event) {
		Connection sender = event.getSender();
		if (!(sender instanceof ProxiedPlayer)) {
			return;
		}
		if (event.isCancelled()) {
			debugPrematurelyDenied(event);
			return;
		}
		ProxiedPlayer player = (ProxiedPlayer) sender;
		InetAddress address = addressReporter.getAddress(player);
		String command = (event.isCommand()) ? event.getMessage().substring(1) : event.getMessage();
		begin(event, enforcer.checkChat(player.getUniqueId(), address, command));
	}

	@Override
	protected boolean isAllowed(ChatEvent event) {
		return !event.isCancelled();
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onChatHigh(ChatEvent event) {
		if (!(event.getSender() instanceof ProxiedPlayer)) {
			return;
		}
		SendableMessage message = withdraw(event);
		if (message == null) {
			debugResultPermitted(event);
			return;
		}
		event.setCancelled(true);
		handle.sendMessage(event.getSender(), message);
	}
	
}
