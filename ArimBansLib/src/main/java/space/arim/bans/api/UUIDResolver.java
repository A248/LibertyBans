/*
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api;

import java.util.UUID;

import space.arim.bans.api.exception.PlayerNotFoundException;
import space.arim.registry.Registrable;

public interface UUIDResolver extends Registrable {
	
	/**
	 * Use this to get a UUID from a playername
	 * 
	 * <br><br><b>This is a blocking operation if query parameter is true</b>.
	 * If you wish to use querying, run inside an async thread!
	 * 
	 * @param name - the name to be resolved
	 * @param query - whether to check webservers, like the Mojang API
	 * @return the uuid of the corresponding player
	 * @throws PlayerNotFoundException if the name could not be resolved to a uuid
	 */
	UUID resolveName(String name, boolean query) throws PlayerNotFoundException;
	
	/**
	 * Use this to get a playername from a UUID
	 * 
	 * <br><br><b>This is a blocking operation if query parameter is true</b>.
	 * If you wish to use querying, run inside an async thread!
	 * 
	 * @param uuid - the uuid to be resolved
	 * @param query - whether to check webservers, like the Mojang API
	 * @return the name of the corresponding player
	 * @throws PlayerNotFoundException if the uuid could not be resolved to a name
	 */
	String resolveUUID(UUID uuid, boolean query) throws PlayerNotFoundException;
	
}
