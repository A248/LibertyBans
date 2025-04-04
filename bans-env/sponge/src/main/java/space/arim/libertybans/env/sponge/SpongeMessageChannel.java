/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

package space.arim.libertybans.env.sponge;

import jakarta.inject.Inject;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.network.channel.raw.play.RawPlayDataChannel;
import space.arim.libertybans.core.env.EnvMessageChannel;
import space.arim.libertybans.core.env.message.PluginMessage;
import space.arim.libertybans.core.env.message.PluginMessageInput;
import space.arim.libertybans.core.env.message.PluginMessageOutput;
import space.arim.libertybans.env.sponge.plugin.ChannelFacade;

import java.io.IOException;
import java.util.function.Consumer;

public final class SpongeMessageChannel implements EnvMessageChannel<ChannelFacade.Handler> {

	private final Game game;
	private final ChannelFacade channelFacade;

	@Inject
	public SpongeMessageChannel(Game game, ChannelFacade channelFacade) {
		this.game = game;
		this.channelFacade = channelFacade;
	}

	private RawPlayDataChannel channel() {
		return game.channelManager()
				.ofType(ResourceKey.of("bungeecord", "main"), RawDataChannel.class)
				.play();
	}

	<D> boolean sendPluginMessage(ServerPlayer player, PluginMessage<D, ?> pluginMessage, D data) {
		var channel = channel();
		boolean supported = channel.isSupportedBy(player.connection());
		//
		// 1. The backend server must NOT be in online mode
		// 2. The channel must be supported
		//
		boolean canSend = !game.server().isOnlineModeEnabled() && supported;
		if (canSend) {
			channel.sendTo(player, (buffer) -> {
				pluginMessage.writeTo(data, new ChannelBufAsOutput(buffer));
			}).exceptionally((ex) -> {
				LoggerFactory.getLogger(getClass()).error("Failed to send plugin message", ex);
				return null;
			});
		}
		return canSend;
	}

	@Override
	public void installHandler(ChannelFacade.Handler handler) {
		handler.install(channel());
	}

	@Override
	public void uninstallHandler(ChannelFacade.Handler handler) {
		handler.uninstall(channel());
	}

	@Override
	public <R> ChannelFacade.Handler createHandler(Consumer<R> acceptor, PluginMessage<?, R> pluginMessage) {
		return channelFacade.makeHandler(new Adapter<>(acceptor, pluginMessage));
	}

	record Adapter<R>(Consumer<R> acceptor, PluginMessage<?, R> pluginMessage) implements ChannelFacade.Adapter {

		@Override
		public void handlePayload(ChannelBuf data) {
			pluginMessage.readFrom(new ChannelBufAsInput(data)).ifPresent(acceptor);
		}
	}

	private record ChannelBufAsOutput(ChannelBuf buffer) implements PluginMessageOutput {
		@Override
		public void writeUTF(String utf) throws IOException {
			buffer.writeUTF(utf);
		}
	}

	private record ChannelBufAsInput(ChannelBuf buffer) implements PluginMessageInput {
		@Override
		public String readUTF() throws IOException {
			return buffer.readUTF();
		}
	}

}
