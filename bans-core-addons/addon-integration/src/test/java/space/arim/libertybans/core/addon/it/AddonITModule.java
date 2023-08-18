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

package space.arim.libertybans.core.addon.it;

import jakarta.inject.Singleton;
import space.arim.libertybans.core.env.EnvMessageChannel;
import space.arim.libertybans.core.env.EnvServerNameDetection;
import space.arim.libertybans.core.importing.PlatformImportSource;
import space.arim.libertybans.core.selector.cache.MuteCache;
import space.arim.libertybans.core.selector.cache.OnDemandMuteCache;
import space.arim.omnibus.DefaultOmnibus;
import space.arim.omnibus.Omnibus;

public class AddonITModule {

	@Singleton
	public Omnibus omnibus() {
		return new DefaultOmnibus();
	}

	public MuteCache muteCache(OnDemandMuteCache muteCache) {
		return muteCache;
	}

	public EnvMessageChannel<?> messageChannel(EnvMessageChannel.NoOp messageChannel) {
		return messageChannel;
	}

	public EnvServerNameDetection serverNameDetection() {
		return (scopeManager) -> {};
	}

	public PlatformImportSource platformImportSource() {
		throw new UnsupportedOperationException("PlatformImportSource not available");
	}
}
