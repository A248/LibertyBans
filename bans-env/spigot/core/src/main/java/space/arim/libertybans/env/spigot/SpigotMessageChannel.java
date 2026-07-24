/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

package space.arim.libertybans.env.spigot;

import jakarta.inject.Inject;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import space.arim.libertybans.core.env.EnvMessageChannel;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.libertybans.core.env.PluginMessageAsBytes;
import space.arim.libertybans.core.env.message.PluginMessage;

import java.util.function.Consumer;

public final class SpigotMessageChannel implements PlatformListener, EnvMessageChannel<PluginMessageListener> {

	private final Plugin plugin;

	static final String BUNGEE_CHANNEL = "BungeeCord";

	@Inject
	public SpigotMessageChannel(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void register() {
		plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
	}

	@Override
	public void unregister() {
		plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, BUNGEE_CHANNEL);
	}

	<D> boolean sendPluginMessage(Player player, PluginMessage<D, ?> pluginMessage, D data) {
		boolean listened = player.getListeningPluginChannels().contains(BUNGEE_CHANNEL);
		//
		// 1. The backend server must NOT be in online mode
		// 2. The channel must be listened on
		//
		boolean canSend = !plugin.getServer().getOnlineMode() && listened;
		if (canSend) {
			player.sendPluginMessage(
					plugin, BUNGEE_CHANNEL,
					new PluginMessageAsBytes<>(pluginMessage).generateBytes(data)
			);
		}
		return canSend;
	}

	@Override
	public void installHandler(PluginMessageListener handler) {
		plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, BUNGEE_CHANNEL, handler);
	}

	@Override
	public void uninstallHandler(PluginMessageListener handler) {
		plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, BUNGEE_CHANNEL, handler);
	}

	@Override
	public <R> PluginMessageListener createHandler(Consumer<R> acceptor, PluginMessage<?, R> pluginMessage) {
		return new Handler<>(acceptor, pluginMessage);
	}

	record Handler<R>(Consumer<R> acceptor, PluginMessage<?, R> pluginMessage) implements PluginMessageListener {

		@Override
		public void onPluginMessageReceived(String channel, Player player, byte[] message) {
			if (channel.equals(BUNGEE_CHANNEL)) {
				new PluginMessageAsBytes<>(pluginMessage)
						.readBytes(message)
						.ifPresent(acceptor);
			}
		}
	}

}
