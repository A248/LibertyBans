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

package space.arim.libertybans.env.sponge.plugin;

import org.spongepowered.api.network.EngineConnectionSide;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.channel.raw.play.RawPlayDataChannel;
import org.spongepowered.api.network.channel.raw.play.RawPlayDataHandler;

public record ChannelFacadeApi8() implements ChannelFacade {

    @Override
    public Handler makeHandler(Adapter adapter) {
        return new ChannelHandlerApi8(new RawHandler(adapter));
    }

    record ChannelHandlerApi8(RawHandler rawHandler) implements Handler {
        @Override
        public void install(RawPlayDataChannel channel) {
            channel.addHandler(EngineConnectionSide.SERVER, rawHandler);
        }

        @Override
        public void uninstall(RawPlayDataChannel channel) {
            channel.removeHandler(rawHandler);
        }
    }

    record RawHandler(Adapter adapter) implements RawPlayDataHandler<ServerSideConnection> {
        @Override
        public void handlePayload(ChannelBuf data, ServerSideConnection connection) {
            adapter.handlePayload(data);
        }
    }
}
