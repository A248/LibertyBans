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
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.util.List;
import java.util.UUID;

/**
 * Enables detecting alts leveraging all the capabilities provided by the implementation
 *
 */
public interface AccountSupervisor {

	/**
	 * Begins to detects alts for the given player
	 *
	 * @param uuid the player's UUID
	 * @param address the player's address
	 * @return a detection query builder
	 */
	AltDetectionQuery.Builder detectAlts(UUID uuid, NetworkAddress address);

	/**
	 * Finds accounts matching the given UUID
	 *
	 * @param uuid the uuid
	 * @return all matching accounts
	 */
	CentralisedFuture<List<? extends KnownAccount>> findAccountsMatching(UUID uuid);

	/**
	 * Finds accounts matching the given address
	 *
	 * @param address the address
	 * @return all matching accounts
	 */
	CentralisedFuture<List<? extends KnownAccount>> findAccountsMatching(NetworkAddress address);

	/**
	 * Finds accounts matching the given UUID or address
	 *
	 * @param uuid the uuid
	 * @param address the address
	 * @return all accounts matching the UUID OR the address
	 */
	CentralisedFuture<List<? extends KnownAccount>> findAccountsMatching(UUID uuid, NetworkAddress address);

}
