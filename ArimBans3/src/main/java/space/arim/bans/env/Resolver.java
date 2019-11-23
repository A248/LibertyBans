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
package space.arim.bans.env;

import java.util.UUID;

import org.json.simple.JSONObject;

import space.arim.bans.api.exception.NoGeoIpException;
import space.arim.bans.api.exception.PlayerNotFoundException;
import space.arim.bans.internal.Configurable;

public interface Resolver extends AutoCloseable, Configurable {
	UUID uuidFromName(final String name) throws PlayerNotFoundException;
	
	String nameFromUUID(final UUID playeruuid) throws PlayerNotFoundException;
	
	JSONObject lookupIp(final String address) throws IllegalArgumentException, NoGeoIpException;
}
