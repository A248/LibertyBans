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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.libertybans.core.env.SimpleEnvUserResolver;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FabricUserResolver extends SimpleEnvUserResolver {

    private final FactoryOfTheFuture factoryOfTheFuture;
    private final MinecraftServer server;

    @Inject
    public FabricUserResolver(FactoryOfTheFuture factoryOfTheFuture, MinecraftServer server) {
        this.factoryOfTheFuture = factoryOfTheFuture;
        this.server = server;
    }

    @Override
    protected <U> CentralisedFuture<U> performLookup(Supplier<U> rootImplementation) {
        return factoryOfTheFuture.supplySync(rootImplementation);
    }

    private <V> Optional<ServerPlayerEntity> getPlayer(Function<PlayerManager, @Nullable ServerPlayerEntity> getter) {
        PlayerManager playerManager = server.getPlayerManager();
        if (playerManager == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(getter.apply(playerManager));
    }

    @Override
    protected Optional<UUID> lookupUUID0(String name) {
        return getPlayer(playerMgr -> playerMgr.getPlayer(name))
                .map(ServerPlayerEntity::getUuid);
    }

    @Override
    protected Optional<String> lookupName0(UUID uuid) {
        return getPlayer(playerMgr -> playerMgr.getPlayer(uuid))
                .map(player -> player.getGameProfile().getName());
    }

    @Override
    protected Optional<InetAddress> lookupAddress0(String name) {
        return getPlayer(playerMgr -> playerMgr.getPlayer(name))
                .flatMap(player -> {
                    SocketAddress socketAddress = player.networkHandler.getConnectionAddress();
                    if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
                        return Optional.of(inetSocketAddress.getAddress());
                    } else {
                        return Optional.empty();
                    }
                });
    }

    @Override
    protected Optional<UUIDAndAddress> lookupPlayer0(String name) {
        return getPlayer(playerMgr -> playerMgr.getPlayer(name))
                .flatMap(player -> {
                    SocketAddress socketAddress = player.networkHandler.getConnectionAddress();
                    if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
                        return Optional.of(new UUIDAndAddress(player.getUuid(), inetSocketAddress.getAddress()));
                    } else {
                        return Optional.empty();
                    }
                });
    }

    @Override
    protected Optional<InetAddress> lookupCurrentAddress0(UUID uuid) {
        return getPlayer(playerMgr -> playerMgr.getPlayer(uuid))
                .flatMap(player -> {
                    SocketAddress socketAddress = player.networkHandler.getConnectionAddress();
                    if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
                        return Optional.of(inetSocketAddress.getAddress());
                    } else {
                        return Optional.empty();
                    }
                });
    }
}
