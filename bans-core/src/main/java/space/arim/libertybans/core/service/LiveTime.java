/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.core.service;

import com.github.benmanes.caffeine.cache.Ticker;

import java.time.Clock;
import java.time.Instant;

/**
 * A live timer, commonly used at runtime
 *
 */
public final class LiveTime implements Time {

	public static final LiveTime INSTANCE = new LiveTime();

	private LiveTime() {
	}

	@Override
	public long currentTime() {
		return System.currentTimeMillis() / 1_000L;
	}

	@Override
	public Instant currentTimestamp() {
		return Instant.now();
	}

	@Override
	public long arbitraryNanoTime() {
		return System.nanoTime();
	}

	@Override
	public Ticker toCaffeineTicker() {
		return Ticker.systemTicker();
	}

	@Override
	public Clock toJdkClock() {
		return Clock.systemUTC();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ".INSTANCE";
	}
}
