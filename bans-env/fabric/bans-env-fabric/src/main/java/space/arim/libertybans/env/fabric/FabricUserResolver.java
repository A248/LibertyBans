/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.libertybans.core.env.SimpleEnvUserResolver;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FabricUserResolver extends SimpleEnvUserResolver {

    private final FactoryOfTheFuture factoryOfTheFuture;
    private final ServerProvide serverProvide;

    @Inject
    public FabricUserResolver(FactoryOfTheFuture factoryOfTheFuture, ServerProvide serverProvide) {
        this.factoryOfTheFuture = factoryOfTheFuture;
        this.serverProvide = serverProvide;
    }

    @Override
    protected <U> CentralisedFuture<U> performLookup(Supplier<U> rootImplementation) {
        return factoryOfTheFuture.supplySync(rootImplementation);
    }

    private Optional<ServerPlayer> getPlayer(Function<PlayerList, @Nullable ServerPlayer> getter) {
        PlayerList playerList = serverProvide.get().getPlayerList();
        if (playerList == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(getter.apply(playerList));
    }

    @Override
    protected Optional<UUID> lookupUUID0(String name) {
        return getPlayer(playerMgr -> playerMgr.getPlayer(name))
                .map(ServerPlayer::getUUID);
    }

    @Override
    protected Optional<String> lookupName0(UUID uuid) {
        return getPlayer(playerMgr -> playerMgr.getPlayer(uuid))
                .map(player -> player.getGameProfile().name());
    }

    InetAddress getAddress(ServerPlayer player) {
        return FabricPlatformAccess.getAddress(player.connection.getRemoteAddress());
    }

    @Override
    protected Optional<InetAddress> lookupAddress0(String name) {
        return getPlayer(playerMgr -> playerMgr.getPlayer(name)).map(this::getAddress);
    }

    @Override
    protected Optional<UUIDAndAddress> lookupPlayer0(String name) {
        return getPlayer(playerMgr -> playerMgr.getPlayer(name))
                .map(player -> new UUIDAndAddress(player.getUUID(), getAddress(player)));
    }

    @Override
    protected Optional<InetAddress> lookupCurrentAddress0(UUID uuid) {
        return getPlayer(playerMgr -> playerMgr.getPlayer(uuid)).map(this::getAddress);
    }
}
