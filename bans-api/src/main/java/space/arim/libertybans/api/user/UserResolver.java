/* 
 * LibertyBans-api
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-api. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.api.user;

import java.util.Optional;
import java.util.UUID;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

/**
 * Provides the capability to resolve UUIDs and names.
 * 
 * @author A248
 *
 */
public interface UserResolver {

	/**
	 * Performs a full lookup of a UUID from a player name. May contact
	 * external APIs such as the Mojang API.
	 * 
	 * @param name the player name
	 * @return a future yielding the UUID or an empty optional if not found
	 */
	CentralisedFuture<Optional<UUID>> lookupUUID(String name);

	/**
	 * Performs a full lookup of a player name from a UUID. May contact
	 * external APIs such as the Mojang API.
	 * 
	 * @param uuid the uuid
	 * @return a future yielding the player name or an empty optional if not found
	 */
	CentralisedFuture<Optional<String>> lookupName(UUID uuid);

}
