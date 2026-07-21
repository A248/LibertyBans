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
import jakarta.inject.Singleton;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import space.arim.libertybans.core.scope.ServerNameListener;
import space.arim.libertybans.core.selector.Guardian;

@Singleton
public final class JoinListener extends FabricListener implements ServerPlayerEvents.Join {

    private final Guardian guardian;
    private final ServerNameListener<ServerPlayer, ?> serverNameListener;
    private final FabricEnforcer fabricEnforcer;

    @Inject
    public JoinListener(Guardian guardian, ServerNameListener<ServerPlayer, ?> serverNameListener,
                        FabricEnforcer fabricEnforcer) {
        this.guardian = guardian;
        this.serverNameListener = serverNameListener;
        this.fabricEnforcer = fabricEnforcer;
    }

    @Override
    void register0() {
        serverNameListener.register();
        ServerPlayerEvents.JOIN.register(Namespacing.ownIdentifier("join"), this);
    }

    @Override
    public void onJoin(@NonNull ServerPlayer player) {
        guardian.onJoin(player, fabricEnforcer);
        serverNameListener.onJoin(player, fabricEnforcer);
    }
}
