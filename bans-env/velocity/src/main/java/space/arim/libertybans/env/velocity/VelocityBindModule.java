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

package space.arim.libertybans.env.velocity;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import jakarta.inject.Singleton;
import space.arim.api.env.PlatformHandle;
import space.arim.api.env.velocity.VelocityPlatformHandle;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.EnvMessageChannel;
import space.arim.libertybans.core.env.EnvServerNameDetection;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.env.Environment;
import space.arim.libertybans.core.importing.PlatformImportSource;
import space.arim.libertybans.core.selector.cache.MuteCache;
import space.arim.libertybans.core.selector.cache.OnDemandMuteCache;

public class VelocityBindModule {

	@Singleton
	public PlatformHandle handle(PluginContainer plugin, ProxyServer server) {
		return VelocityPlatformHandle.create(plugin, server);
	}

	public MuteCache muteCache(OnDemandMuteCache muteCache) {
		return muteCache;
	}

	public Environment environment(VelocityEnv env) {
		return env;
	}

	public EnvEnforcer<?> enforcer(VelocityEnforcer enforcer) {
		return enforcer;
	}

	public EnvUserResolver resolver(VelocityUserResolver resolver) {
		return resolver;
	}

	public EnvMessageChannel<?> messageChannel(VelocityMessageChannel messageChannel) {
		return messageChannel;
	}

	public EnvServerNameDetection serverNameDetection() {
		return (scopeManager) -> scopeManager.detectServerName("proxy");
	}

	public PlatformImportSource platformImportSource() {
		throw new UnsupportedOperationException("It is impossible to import from vanilla on Velocity");
	}

}
