/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.it.env;

import jakarta.inject.Singleton;
import space.arim.api.env.PlatformHandle;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.env.Environment;
import space.arim.libertybans.core.importing.PlatformImportSource;
import space.arim.libertybans.core.selector.cache.MuteCache;
import space.arim.libertybans.core.selector.cache.OnDemandMuteCache;
import space.arim.libertybans.it.env.platform.QuackPlatform;
import space.arim.omnibus.DefaultOmnibus;
import space.arim.omnibus.Omnibus;

public class QuackBindModule {

	@Singleton
	public Omnibus omnibus() {
		return new DefaultOmnibus();
	}

	@Singleton
	public PlatformHandle handle(QuackPlatform platform) {
		return new QuackHandle(platform);
	}

	public MuteCache muteCache(OnDemandMuteCache muteCache) {
		return muteCache;
	}

	public Environment environment(QuackEnv env) {
		return env;
	}

	public EnvEnforcer<?> enforcer(QuackEnforcer enforcer) {
		return enforcer;
	}

	public EnvUserResolver resolver(QuackUserResolver resolver) {
		return resolver;
	}

	public PlatformImportSource platformImportSource() {
		throw new UnsupportedOperationException("PlatformImportSource not available");
	}

}
