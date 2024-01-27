/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.uuid;

import java.util.Optional;
import java.util.UUID;

import space.arim.libertybans.core.Part;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.user.UserResolver;

public interface UUIDManager extends UserResolver, Part {

	void addCache(UUID uuid, String name);

	/**
	 * Looks up an address from a player name
	 * 
	 * @param name the name of the player
	 * @return a future which yields the address or {@code null} if none was found
	 */
	CentralisedFuture<NetworkAddress> lookupAddress(String name);

	/**
	 * Performs a full lookup of a uuid from an exact player name. <br>
	 * Differs from {@link #lookupUUID(String)} in that this method
	 * assumes the name to be exactly cased, in order that offline UUIDs
	 * may be calculated from it
	 *
	 * @param name the exact player name. Must be correctly cased
	 * @return a future yielding the uuid or an empty optional if not found
	 */
	CentralisedFuture<Optional<UUID>> lookupUUIDFromExactName(String name);

	/**
	 * Looks up player details from a player name
	 *
	 * @param name the name of the player
	 * @return a future yielding the user details or an empty optional if not found
	 */
	CentralisedFuture<Optional<UUIDAndAddress>> lookupPlayer(String name);

	/**
	 * Looks up player details from a UUID
	 *
	 * @param uuid the uuid of the player
	 * @return a future yielding the latest address or an empty optional if not found
	 */
	CentralisedFuture<Optional<NetworkAddress>> lookupLastAddress(UUID uuid);

}
