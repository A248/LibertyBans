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
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;

@Singleton
public final class ServerAudiences extends FabricListener {

    private final ServerProvide serverProvide;
    private MinecraftServerAudiences serverAudiences;

    @Inject
    public ServerAudiences(ServerProvide serverProvide) {
        this.serverProvide = serverProvide;
    }

    MinecraftServerAudiences getServerAudiences() {
        MinecraftServerAudiences serverAudiences = this.serverAudiences;
        if (serverAudiences == null) {
            throw new IllegalStateException("Not initialized");
        }
        return serverAudiences;
    }

    @Override
    void register0() {
        serverAudiences = MinecraftServerAudiences.of(serverProvide.get());
    }
}
