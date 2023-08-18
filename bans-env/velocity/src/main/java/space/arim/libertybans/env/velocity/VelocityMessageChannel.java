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

package space.arim.libertybans.env.velocity;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import jakarta.inject.Inject;
import space.arim.libertybans.core.env.EnvMessageChannel;
import space.arim.libertybans.core.env.PluginMessageAsBytes;
import space.arim.libertybans.core.env.message.PluginMessage;

import java.util.function.Consumer;

public final class VelocityMessageChannel implements EnvMessageChannel<EventHandler<PluginMessageEvent>> {

	private final PluginContainer plugin;
	private final ProxyServer server;

	private static final ChannelIdentifier BUNGEE_CHANNEL = MinecraftChannelIdentifier.create("bungeecord", "main");

	@Inject
	public VelocityMessageChannel(PluginContainer plugin, ProxyServer server) {
		this.plugin = plugin;
		this.server = server;
	}

	<D> void sendPluginMessage(Player player, PluginMessage<D, ?> pluginMessage, D data) {
		player.getCurrentServer().ifPresent((server) -> {
			server.sendPluginMessage(
					BUNGEE_CHANNEL,
					new PluginMessageAsBytes<>(pluginMessage).generateBytes(data)
			);
		});
	}

	@Override
	public void installHandler(EventHandler<PluginMessageEvent> handler) {
		server.getEventManager().register(plugin, PluginMessageEvent.class, handler);
	}

	@Override
	public void uninstallHandler(EventHandler<PluginMessageEvent> handler) {
		server.getEventManager().unregister(plugin, handler);
	}

	@Override
	public <R> EventHandler<PluginMessageEvent> createHandler(Consumer<R> acceptor, PluginMessage<?, R> pluginMessage) {
		class AsHandler implements EventHandler<PluginMessageEvent> {

			@Override
			public void execute(PluginMessageEvent event) {
				if (event.getSource() instanceof ServerConnection && event.getIdentifier().equals(BUNGEE_CHANNEL)) {
					new PluginMessageAsBytes<>(pluginMessage)
							.readBytes(event.getData())
							.ifPresent(acceptor);
				}
			}
		}
		return new AsHandler();
	}
}
