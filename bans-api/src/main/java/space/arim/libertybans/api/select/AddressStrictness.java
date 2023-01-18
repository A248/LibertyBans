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
 * How address punishments should be enforced
 *
 */
public enum AddressStrictness {
	/**
	 * Player's current address must match target address
	 * 
	 */
	LENIENT,
	/**
	 * Any of player's past addresses may match target address
	 * 
	 */
	NORMAL,
	/**
	 * Any of player's past addresses may match any address related to the target address
	 * by a common player
	 *
	 */
	STERN,
	/**
	 * Same as STERN and also treats every user ban as an IP ban
	 *
	 */
	STRICT
}
