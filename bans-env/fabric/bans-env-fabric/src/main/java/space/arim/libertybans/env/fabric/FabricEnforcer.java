/*
 * LibertyBans-fabric
 * Copyright © 2026 Anand Beh
 *
 * LibertyBans-fabric is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibertyBans-fabric is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with LibertyBans-fabric. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Lesser General Public License.
 */

package space.arim.libertybans.env.fabric;

import jakarta.inject.Inject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AbstractEnvEnforcer;
import space.arim.libertybans.core.env.Interlocutor;
import space.arim.libertybans.core.env.PluginMessageAsBytes;
import space.arim.libertybans.core.env.message.PluginMessage;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class FabricEnforcer extends AbstractEnvEnforcer<ServerPlayer> {

    private final ServerProvide serverProvide;
    private final FabricUserResolver userResolver;
    private final ServerAudiences serverAudiences;

    @Inject
    public FabricEnforcer(FactoryOfTheFuture futuresFactory, InternalFormatter formatter, Interlocutor interlocutor,
                          ServerProvide serverProvide, FabricUserResolver userResolver, ServerAudiences serverAudiences) {
        super(futuresFactory, formatter, interlocutor, AudienceRepresenter.identity());
        this.serverProvide = serverProvide;
        this.userResolver = userResolver;
        this.serverAudiences = serverAudiences;
    }

    @SuppressWarnings("unchecked")
    private CentralisedFuture<Void> runSync(Runnable command) {
        // Technically an inaccurate cast, but it will never matter
        return (CentralisedFuture<Void>) futuresFactory().runSync(command);
    }

    @Override
    public CentralisedFuture<Void> doForPlayerIfOnline(UUID uuid, Consumer<ServerPlayer> callback) {
        return runSync(() -> {
            PlayerList playerList = serverProvide.get().getPlayerList();
            ServerPlayer player;
            if (playerList != null && (player = playerList.getPlayer(uuid)) != null) {
                callback.accept(player);
            }
        });
    }

    @Override
    public CentralisedFuture<Void> doForAllPlayers(Consumer<Collection<? extends ServerPlayer>> action) {
        return runSync(() -> {
            PlayerList playerList = serverProvide.get().getPlayerList();
            List<ServerPlayer> players = playerList == null ? List.of() : playerList.getPlayers();
            action.accept(players);
        });
    }

    @Override
    public <D> boolean sendPluginMessageIfListening(ServerPlayer player, PluginMessage<D, ?> pluginMessage, D data) {
        if (!ServerPlayNetworking.canSend(player, BungeeMessage.TYPE)) {
            return false;
        }
        byte[] dataBytes = new PluginMessageAsBytes<>(pluginMessage).generateBytes(data);
        ServerPlayNetworking.send(player, new BungeeMessage(dataBytes));
        return true;
    }

    @Override
    public UUID getUniqueIdFor(ServerPlayer player) {
        return player.getUUID();
    }

    @Override
    public InetAddress getAddressFor(ServerPlayer player) {
        return userResolver.getAddress(player);
    }

    @Override
    public String getNameFor(ServerPlayer player) {
        return player.getGameProfile().name();
    }

    @Override
    public String getPlayableServerName(ServerPlayer player) {
        return null;
    }

    @Override
    public boolean hasPermission(ServerPlayer player, String permission) {
        return player.permissions().hasPermission(Namespacing.parsePermission(permission));
    }

    @Override
    public CompletableFuture<Void> executeConsoleCommand(String command) {
        MinecraftServer server = serverProvide.get();
        return runSync(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), '/' + command));
    }

    @Override
    public void kickPlayer(ServerPlayer player, Component message) {
        player.connection.disconnect(serverAudiences.getServerAudiences().asNative(message));
    }
}
