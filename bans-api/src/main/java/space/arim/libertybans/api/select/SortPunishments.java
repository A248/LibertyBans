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

package space.arim.libertybans.api.select;

/**
 * Defines how punishments may be ordered. <br>
 * <br>
 * Combining multiple values of {@code SortPunishments} directives into an array implies that defacto ordering
 * will depend on each directive in sequence. Punishments will be initially sorted by the first ordering directive;
 * punishments considered equal by the first directive will be sorted by the second ordering directive, and so on.
 *
 */
public enum SortPunishments {
	/**
	 * Punishments will be returned in descending date of enaction
	 */
	NEWEST_FIRST,
	/**
	 * Punishments will be returned in ascending date of enaction
	 */
	OLDEST_FIRST,
	/**
	 * Punishments will be returned in descending date of end time, permanent punishments coming first
	 */
	LATEST_END_DATE_FIRST,
	/**
	 * Punishments will be returned in ascending date of end time, permanent punishments coming last
	 */
	SOONEST_END_DATE_FIRST
}
