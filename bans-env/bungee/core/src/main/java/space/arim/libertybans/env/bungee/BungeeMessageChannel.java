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

package space.arim.libertybans.env.bungee;

import jakarta.inject.Inject;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import space.arim.libertybans.core.env.EnvMessageChannel;
import space.arim.libertybans.core.env.PluginMessageAsBytes;
import space.arim.libertybans.core.env.message.PluginMessage;

import java.util.function.Consumer;

public final class BungeeMessageChannel implements EnvMessageChannel<Listener> {

	private final Plugin plugin;

	private static final String BUNGEE_CHANNEL = "BungeeCord";

	@Inject
	public BungeeMessageChannel(Plugin plugin) {
		this.plugin = plugin;
	}

	<D> void sendPluginMessage(ProxiedPlayer player, PluginMessage<D, ?> pluginMessage, D data) {
		Server server = player.getServer();
		if (server != null) {
			server.sendData(BUNGEE_CHANNEL, new PluginMessageAsBytes<>(pluginMessage).generateBytes(data));
		}
	}

	@Override
	public void installHandler(Listener handler) {
		plugin.getProxy().getPluginManager().registerListener(plugin, handler);
	}

	@Override
	public void uninstallHandler(Listener handler) {
		plugin.getProxy().getPluginManager().unregisterListener(handler);
	}

	@Override
	public <R> Listener createHandler(Consumer<R> acceptor, PluginMessage<?, R> pluginMessage) {
		return new Handler<>(acceptor, pluginMessage);
	}

	// Public for reflection purposes
	public record Handler<R>(Consumer<R> acceptor, PluginMessage<?, R> pluginMessage) implements Listener {

		@EventHandler
		public void onReceive(PluginMessageEvent event) {
			if (event.getSender() instanceof Server && event.getTag().equals(BUNGEE_CHANNEL)) {
				new PluginMessageAsBytes<>(pluginMessage)
						.readBytes(event.getData())
						.ifPresent(acceptor);
			}
 		}
	}

}
