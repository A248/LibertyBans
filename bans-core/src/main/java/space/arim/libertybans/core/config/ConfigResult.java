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

package space.arim.libertybans.core.config;

import java.util.Arrays;
import java.util.Collection;

public enum ConfigResult {
	/**
	 * Loaded the specified configuration successfully
	 */
	SUCCESS_LOADED,
	/**
	 * Loaded the default configuration because the path did not exist
	 */
	SUCCESS_WITH_DEFAULTS,
	/**
	 * The user's configuration is invalid (wrong type, bad yaml syntax)
	 */
	USER_ERROR,
	/**
	 * An I/O exception occurred
	 */
	IO_ERROR;

	/**
	 * Determines whether this was successful: no user errors or I/O errors
	 *
	 * @return true if successful
	 */
	public boolean isSuccess() {
		return this == SUCCESS_LOADED || this == SUCCESS_WITH_DEFAULTS;
	}

	static ConfigResult combinePessimistically(ConfigResult...results) {
		return combinePessimistically(Arrays.asList(results));
	}

	public static ConfigResult combinePessimistically(Collection<ConfigResult> results) {
		int maxOrdinal = 0;
		for (ConfigResult result : results) {
			maxOrdinal = Math.max(maxOrdinal, result.ordinal());
		}
		return values()[maxOrdinal];
	}

}
