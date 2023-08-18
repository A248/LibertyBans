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

public interface SettableTime extends Time {

	/**
	 * Sets the time to the given timestamp
	 *
	 * @param timestamp the timestamp to use
	 */
	void setTimestamp(Instant timestamp);

	/**
	 * Sets the time to a new time with the given progression applied
	 *
	 * @param progression the progression
	 */
	void advanceBy(Duration progression);

	/**
	 * Sets the time back to the original test timestamp
	 *
	 */
	void reset();

}
