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

import java.time.Instant;

/**
 * A fixed timer
 *
 */
public final class FixedTime implements Time {

	private final long timestamp;

	private FixedTime(long timestamp) {
		this.timestamp = timestamp;
	}

	public FixedTime(Instant timestamp) {
		this(timestamp.getEpochSecond());
	}

	@Override
	public long currentTime() {
		return timestamp;
	}

	@Override
	public long arbitraryNanoTime() {
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FixedTime fixed = (FixedTime) o;
		return timestamp == fixed.timestamp;
	}

	@Override
	public int hashCode() {
		return (int) (timestamp ^ (timestamp >>> 32));
	}

	@Override
	public String toString() {
		return "Fixed{" +
				"timestamp=" + timestamp +
				'}';
	}
}
