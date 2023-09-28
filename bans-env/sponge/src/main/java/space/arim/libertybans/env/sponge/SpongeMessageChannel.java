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

package space.arim.libertybans.env.sponge;

import jakarta.inject.Inject;
import org.slf4j.LoggerFactory;
import org.spongepowered.api.Game;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.network.EngineConnectionSide;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.network.channel.raw.play.RawPlayDataChannel;
import org.spongepowered.api.network.channel.raw.play.RawPlayDataHandler;
import space.arim.libertybans.core.env.EnvMessageChannel;
import space.arim.libertybans.core.env.message.PluginMessage;
import space.arim.libertybans.core.env.message.PluginMessageInput;
import space.arim.libertybans.core.env.message.PluginMessageOutput;

import java.io.IOException;
import java.util.function.Consumer;

public final class SpongeMessageChannel implements EnvMessageChannel<RawPlayDataHandler<ServerSideConnection>> {

	private final Game game;

	@Inject
	public SpongeMessageChannel(Game game) {
		this.game = game;
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
	public void installHandler(RawPlayDataHandler<ServerSideConnection> handler) {
		channel().addHandler(EngineConnectionSide.SERVER, handler);
	}

	@Override
	public void uninstallHandler(RawPlayDataHandler<ServerSideConnection> handler) {
		channel().removeHandler(handler);
	}

	@Override
	public <R> RawPlayDataHandler<ServerSideConnection> createHandler(Consumer<R> acceptor,
																	  PluginMessage<?, R> pluginMessage) {
		return new Handler<>(acceptor, pluginMessage);
	}

	record Handler<R>(Consumer<R> handler, PluginMessage<?, R> pluginMessage)
			implements RawPlayDataHandler<ServerSideConnection> {

		@Override
		public void handlePayload(ChannelBuf data, ServerSideConnection connection) {
			pluginMessage.readFrom(new ChannelBufAsInput(data)).ifPresent(handler);
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
