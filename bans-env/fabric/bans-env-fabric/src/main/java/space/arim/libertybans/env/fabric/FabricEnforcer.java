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
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kyori.adventure.text.Component;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AbstractEnvEnforcer;
import space.arim.libertybans.core.env.Interlocutor;
import space.arim.libertybans.core.env.PluginMessageAsBytes;
import space.arim.libertybans.core.env.message.PluginMessage;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class FabricEnforcer extends AbstractEnvEnforcer<ServerPlayerEntity> {

    private final MinecraftServer server;
    private final ServerRegistrars serverRegistrars;

    private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

    @Inject
    public FabricEnforcer(FactoryOfTheFuture futuresFactory, InternalFormatter formatter,
                          Interlocutor interlocutor, MinecraftServer server, ServerRegistrars serverRegistrars) {
        super(futuresFactory, formatter, interlocutor, AudienceRepresenter.identity());
        this.server = server;
        this.serverRegistrars = serverRegistrars;
    }

    @SuppressWarnings("unchecked")
    private CentralisedFuture<Void> runSync(Runnable command) {
        // Technically an inaccurate cast, but it will never matter
        return (CentralisedFuture<Void>) futuresFactory().runSync(command);
    }

    @Override
    public CentralisedFuture<Void> doForPlayerIfOnline(UUID uuid, Consumer<ServerPlayerEntity> callback) {
        return runSync(() -> {
            PlayerManager playerManager = server.getPlayerManager();
            ServerPlayerEntity player;
            if (playerManager != null && (player = playerManager.getPlayer(uuid)) != null) {
                callback.accept(player);
            }
        });
    }

    @Override
    public CentralisedFuture<Void> doForAllPlayers(Consumer<Collection<? extends ServerPlayerEntity>> action) {
        return runSync(() -> {
            PlayerManager playerManager = server.getPlayerManager();
            List<ServerPlayerEntity> players = playerManager == null ? List.of() : playerManager.getPlayerList();
            action.accept(players);
        });
    }

    @Override
    public <D> boolean sendPluginMessageIfListening(ServerPlayerEntity player, PluginMessage<D, ?> pluginMessage, D data) {
        Identifier bungeeChannel = Identifier.of("bungeecord", "main");
        if (!ServerPlayNetworking.canSend(player, bungeeChannel)) {
            return false;
        }
        PayloadTypeRegistry.playS2C().register()
        byte[] dataBytes = new PluginMessageAsBytes<>(pluginMessage).generateBytes(data);
        ServerPlayNetworking.send();
        record BungeeCordMessage<D>(PluginMessage<D, ?> pluginMessage, D data) implements CustomPayload {

            @Override
            public Id<BungeeCordMessage<D>> getId() {
                return new Id<>();
            }
        }
        player.networkHandler.sendPacket(new CustomPayloadS2CPacket(new BungeeCordMessage<>(pluginMessage, data)));
        return false;
    }

    @Override
    public UUID getUniqueIdFor(ServerPlayerEntity player) {
        return player.getUuid();
    }

    @Override
    public InetAddress getAddressFor(ServerPlayerEntity player) {
        SocketAddress socketAddress = player.networkHandler.getConnectionAddress();
        if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
            return inetSocketAddress.getAddress();
        } else {
            // Probably a player connected to an integrated server
            logger.info(
                    "No InetSocketAddress for {}, probably due to running on integrated server. We will use 127.0.0.1.",
                    player.getGameProfile()
            );
            return InetAddress.getLoopbackAddress();
        }
    }

    @Override
    public String getNameFor(ServerPlayerEntity player) {
        return player.getGameProfile().getName();
    }

    @Override
    public String getPlayableServerName(ServerPlayerEntity player) {
        return null;
    }

    @Override
    public boolean hasPermission(ServerPlayerEntity player, String permission) {
        return Permissions.check(player, permission);
    }

    @Override
    public CompletableFuture<Void> executeConsoleCommand(String command) {
        return runSync(() -> {
            server.getCommandManager().executeWithPrefix(server.getCommandSource(), '/' + command);
        });
    }

    @Override
    public void kickPlayer(ServerPlayerEntity player, Component message) {
        player.networkHandler.disconnect(serverRegistrars.getServerAudiences().asNative(message));
    }
}
