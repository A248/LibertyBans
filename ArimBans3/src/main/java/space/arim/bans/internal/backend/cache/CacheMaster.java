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
package space.arim.bans.internal.backend.cache;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.UUID;

import space.arim.bans.api.exception.MissingCacheException;
import space.arim.bans.internal.Replaceable;

public interface CacheMaster extends Replaceable {
	
	ArrayList<String> getIps(UUID playeruuid);
	
	String getName(UUID playeruuid) throws MissingCacheException;
	
	UUID getUUID(String name) throws MissingCacheException;
	
	void update(UUID playeruuid, String name, String ip);
	
	boolean uuidExists(UUID uuid);
	
	boolean hasIp(UUID playeruuid, String ip);
	
	void loadAll(ResultSet data);
}
