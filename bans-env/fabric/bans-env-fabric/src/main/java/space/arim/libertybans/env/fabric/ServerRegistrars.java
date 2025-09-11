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

package space.arim.libertybans.env.fabric;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import space.arim.libertybans.core.env.PlatformListener;

@Singleton
public final class ServerRegistrars implements PlatformListener {

    private final MinecraftServer server;
    private MinecraftServerAudiences serverAudiences;

    @Inject
    public ServerRegistrars(MinecraftServer server) {
        this.server = server;
    }

    MinecraftServerAudiences getServerAudiences() {
        return serverAudiences;
    }

    @Override
    public void register() {
        serverAudiences = MinecraftServerAudiences.of(server);
        PayloadTypeRegistry.playS2C().register(new CustomPayload.Id<>(Identifier.of("bungeecord", "main")), new PacketCodec<RegistryByteBuf, CustomPayload>() {
            @Override
            public CustomPayload decode(RegistryByteBuf buf) {
                return null;
            }

            @Override
            public void encode(RegistryByteBuf buf, CustomPayload value) {

            }
        });
    }

    @Override
    public void unregister() {
        serverAudiences = null;
    }
}
