/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.env.bungee;

import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.env.ParallelisedListener;
import space.arim.libertybans.core.selector.Guardian;

import java.net.InetAddress;

public final class ChatListener extends ParallelisedListener<ChatEvent, Component> implements Listener {

	private final Plugin plugin;
	private final Guardian guardian;
	private final AddressReporter addressReporter;
	private final AudienceRepresenter<CommandSender> audienceRepresenter;

	@Inject
	public ChatListener(Plugin plugin, Guardian guardian,
						AddressReporter addressReporter, AudienceRepresenter<CommandSender> audienceRepresenter) {
		this.plugin = plugin;
		this.guardian = guardian;
		this.addressReporter = addressReporter;
		this.audienceRepresenter = audienceRepresenter;
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
		if (!(sender instanceof ProxiedPlayer player)) {
			return;
		}
		if (event.isCancelled()) {
			debugPrematurelyDenied(event);
			return;
		}
		InetAddress address = addressReporter.getAddress(player);
		String command = (event.isCommand()) ? event.getMessage().substring(1) : null;
		begin(event, guardian.checkChat(player.getUniqueId(), address, command));
	}

	@Override
	protected boolean isAllowed(ChatEvent event) {
		return !event.isCancelled();
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onChatHigh(ChatEvent event) {
		Connection sender = event.getSender();
		if (!(sender instanceof ProxiedPlayer player)) {
			return;
		}
		Component message = withdraw(event);
		if (message == null) {
			debugResultPermitted(event);
			return;
		}
		event.setCancelled(true);
		audienceRepresenter.toAudience(player).sendMessage(message);
	}
	
}
