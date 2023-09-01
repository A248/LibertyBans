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

package space.arim.libertybans.api.user;

import space.arim.libertybans.api.NetworkAddress;

import java.util.Optional;
import java.util.UUID;

/**
 * Base interface for accounts
 *
 */
public interface AccountBase {

	/**
	 * The account UUID
	 *
	 * @return the UUID
	 */
	UUID uuid();

	/**
	 * The latest username for the corresponding user, if it is known
	 *
	 * @return the latest username if there is one
	 */
	Optional<String> latestUsername();

	/**
	 * The account address
	 *
	 * @return the address
	 */
	NetworkAddress address();

}
