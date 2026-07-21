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

import space.arim.api.env.PlatformHandle;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.EnvMessageChannel;
import space.arim.libertybans.core.env.EnvServerNameDetection;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.env.Environment;
import space.arim.libertybans.core.importing.PlatformImportSource;
import space.arim.libertybans.core.selector.cache.AlwaysAvailableMuteCache;
import space.arim.libertybans.core.selector.cache.MuteCache;
import space.arim.libertybans.env.fabric.mod.PlatformAccess;

public class FabricBindModule {

    public PlatformHandle platformHandle(FabricPlatformHandle platformHandle) {
        return platformHandle;
    }

    public MuteCache muteCache(AlwaysAvailableMuteCache muteCache) {
        return muteCache;
    }

    public Environment environment(FabricEnv env) {
        return env;
    }

    public EnvEnforcer<?> enforcer(FabricEnforcer enforcer) {
        return enforcer;
    }

    public EnvUserResolver envUserResolver(FabricUserResolver resolver) {
        return resolver;
    }

    public EnvMessageChannel<?> messageChannel(FabricMessageChannel messageChannel) {
        return messageChannel;
    }

    public EnvServerNameDetection serverNameDetection() {
        return (scopeManager) -> {};
    }

    public PlatformAccess platformAccess(FabricPlatformAccess platformAccess) {
        return platformAccess;
    }

    public PlatformImportSource platformImportSource() {
        throw new UnsupportedOperationException("Importing from vanilla is currently not supported on Fabric.");
    }
}
