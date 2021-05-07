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

/**
 * The tests in this package operate on the premise of 3 users.
 * 2 of them have 1 address in common, and 1 address distinct
 * The other user is completely unrelated. <br>
 * <br>
 * Typically, one or both of the first two users are banned, and
 * the enforcement of such is tested according to a certain
 * {@link AddressStrictness}.
 * 
 */
package space.arim.libertybans.it.test.applicable;

