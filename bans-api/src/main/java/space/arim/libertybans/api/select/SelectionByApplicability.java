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

import space.arim.libertybans.api.NetworkAddress;

import java.util.UUID;

/**
 * A selection that finds punishments applicable to a UUID/address pair.
 *
 */
public interface SelectionByApplicability extends SelectionBase {

	/**
	 * Gets the UUID in terms of which applicability will be considered
	 *
	 * @return the uuid
	 */
	UUID getUUID();

	/**
	 * Gets the address in terms of which applicability will be considered
	 *
	 * @return the address
	 */
	NetworkAddress getAddress();

	/**
	 * Gets the address strictness setting used to calculate applicability
	 *
	 * @return the address strictness used
	 */
	AddressStrictness getAddressStrictness();

}
