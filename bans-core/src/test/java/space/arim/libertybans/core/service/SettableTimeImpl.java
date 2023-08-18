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

package space.arim.libertybans.core.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public final class SettableTimeImpl implements SettableTime {

	private final Instant original;
	/** Milliseconds from the epoch */
	private final AtomicLong timestamp;

	public SettableTimeImpl(Instant timestamp) {
		original = timestamp;
		this.timestamp = new AtomicLong(timestamp.toEpochMilli());
	}

	@Override
	public void setTimestamp(Instant timestamp) {
		this.timestamp.set(timestamp.toEpochMilli());
	}

	@Override
	public void advanceBy(Duration duration) {
		this.timestamp.getAndAdd(duration.toMillis());
	}

	@Override
	public long currentTime() {
		return timestamp.get() / 1_000L; // seconds
	}

	@Override
	public long arbitraryNanoTime() {
		return timestamp.get() * 1_000_000L; // nanoseconds
	}

	@Override
	public void reset() {
		setTimestamp(original);
	}

}
