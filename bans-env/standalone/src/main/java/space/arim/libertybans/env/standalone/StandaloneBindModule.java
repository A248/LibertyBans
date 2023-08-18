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

package space.arim.libertybans.env.standalone;

import jakarta.inject.Singleton;
import space.arim.api.env.PlatformHandle;
import space.arim.api.env.PlatformPluginInfo;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.EnvMessageChannel;
import space.arim.libertybans.core.env.EnvServerNameDetection;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.env.Environment;
import space.arim.libertybans.core.importing.PlatformImportSource;
import space.arim.libertybans.core.selector.cache.MuteCache;
import space.arim.libertybans.core.selector.cache.OnDemandMuteCache;
import space.arim.omnibus.util.concurrent.EnhancedExecutor;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.SimplifiedEnhancedExecutor;

import java.util.concurrent.ForkJoinPool;

public class StandaloneBindModule {

	@Singleton
	public PlatformHandle handle() {
		return new PlatformHandle() {
			@Override
			public FactoryOfTheFuture createFuturesFactory() {
				return new IndifferentFactoryOfTheFuture();
			}

			@Override
			public EnhancedExecutor createEnhancedExecutor() {
				return new SimplifiedEnhancedExecutor() {
					@Override
					public void execute(Runnable command) {
						ForkJoinPool.commonPool().execute(command);
					}
				};
			}

			@Override
			public PlatformPluginInfo getImplementingPluginInfo() {
				return new PlatformPluginInfo(
						// Completely irrelevant
						StandaloneBindModule.this, StandaloneBindModule.class
				);
			}
		};
	}

	public MuteCache muteCache(OnDemandMuteCache muteCache) {
		return muteCache;
	}

	public Environment environment(StandaloneEnv env) {
		return env;
	}

	public EnvEnforcer<?> enforcer(StandaloneEnforcer enforcer) {
		return enforcer;
	}

	public EnvUserResolver resolver(StandaloneResolver resolver) {
		return resolver;
	}

	public EnvMessageChannel<?> messageChannel(EnvMessageChannel.NoOp messageChannel) {
		return messageChannel;
	}

	public EnvServerNameDetection serverNameDetection() {
		return (scopeManager) -> scopeManager.detectServerName("standalone");
	}

	public PlatformImportSource platformImportSource() {
		throw new UnsupportedOperationException("It is impossible to import from vanilla on the standalone application");
	}

}
