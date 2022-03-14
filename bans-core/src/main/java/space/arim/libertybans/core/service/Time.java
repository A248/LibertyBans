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
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Timer which is very similar to {@link Clock}
 *
 */
public interface Time {

	/**
	 * Retrieves the current time in seconds
	 *
	 * @return the current time
	 */
	long currentTime();

	/**
	 * Retrieves the current timestamp
	 *
	 * @return the current timestamp
	 */
	default Instant currentTimestamp() {
		return Instant.ofEpochSecond(currentTime());
	}

	/**
	 * Retrieves the number of nanoseconds since some fixed, arbitrary point in time
	 *
	 * @return the nanoseconds which have passed
	 */
	long arbitraryNanoTime();

	default Ticker toCaffeineTicker() {
		return this::arbitraryNanoTime;
	}

	default Clock toJdkClock() {
		return new Clock() {
			@Override
			public ZoneId getZone() {
				return ZoneOffset.UTC;
			}

			@Override
			public Clock withZone(ZoneId zoneId) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Instant instant() {
				return Time.this.currentTimestamp();
			}
		};
	}
}
