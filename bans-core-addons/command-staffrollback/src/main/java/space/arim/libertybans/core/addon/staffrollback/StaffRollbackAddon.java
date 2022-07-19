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

package space.arim.libertybans.core.addon.staffrollback;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.checkerframework.checker.index.qual.NonNegative;
import space.arim.libertybans.core.addon.AbstractAddon;
import space.arim.libertybans.core.addon.AddonCenter;
import space.arim.libertybans.core.addon.staffrollback.execute.PreparedRollback;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Singleton
public final class StaffRollbackAddon extends AbstractAddon<StaffRollbackConfig> {

	private Cache<UUID, PreparedRollback> confirmations;

	@Inject
	public StaffRollbackAddon(AddonCenter addonCenter) {
		super(addonCenter);
	}

	Cache<UUID, PreparedRollback> confirmationCache() {
		return confirmations;
	}

	@Override
	public void startup() {
		class ConfigExpiration implements Expiry<UUID, PreparedRollback> {

			private long expirationTimeNanos() {
				long timeSeconds = config().confirmation().expirationTimeSeconds();
				return TimeUnit.SECONDS.toNanos(timeSeconds);
			}

			@Override
			public long expireAfterCreate(UUID key, PreparedRollback value, long currentTime) {
				return expirationTimeNanos();
			}

			@Override
			public long expireAfterUpdate(UUID key, PreparedRollback value, long currentTime, @NonNegative long currentDuration) {
				return expirationTimeNanos();
			}

			@Override
			public long expireAfterRead(UUID key, PreparedRollback value, long currentTime, @NonNegative long currentDuration) {
				return Long.MAX_VALUE;
			}
		}
		confirmations = Caffeine.newBuilder()
				.expireAfter(new ConfigExpiration())
				.build();
	}

	@Override
	public void shutdown() {
		confirmations = null;
	}

	@Override
	public Class<StaffRollbackConfig> configInterface() {
		return StaffRollbackConfig.class;
	}

	@Override
	public String identifier() {
		return "command-staffrollback";
	}
}
