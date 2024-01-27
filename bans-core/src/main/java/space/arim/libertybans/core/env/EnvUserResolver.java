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

package space.arim.libertybans.core.env;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;

/**
 * User resolved. Name based lookups are case insensitive
 *
 */
public interface EnvUserResolver {

	CentralisedFuture<Optional<UUID>> lookupUUID(String name);

	CentralisedFuture<Optional<String>> lookupName(UUID uuid);

	CentralisedFuture<Optional<InetAddress>> lookupAddress(String name);

	CentralisedFuture<Optional<UUIDAndAddress>> lookupPlayer(String name);

	CentralisedFuture<Optional<InetAddress>> lookupCurrentAddress(UUID uuid);

}
