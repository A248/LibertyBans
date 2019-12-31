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
package space.arim.bans.internal.backend.resolver;

import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import space.arim.bans.api.exception.MissingCacheException;
import space.arim.bans.api.exception.NoGeoIpException;
import space.arim.bans.internal.Component;

import space.arim.api.framework.UUIDResolver;
import space.arim.api.util.web.GeoIpInfo;

public interface ResolverMaster extends Component, UUIDResolver {
	@Override
	default Class<?> getType() {
		return ResolverMaster.class;
	}
	
	List<String> getIps(UUID playeruuid) throws MissingCacheException;
	
	List<UUID> getPlayers(String address);
	
	String getName(UUID playeruuid) throws MissingCacheException;
	
	UUID getUUID(String name, boolean ignoreCase) throws MissingCacheException;
	
	default UUID getUUID(String name) throws MissingCacheException {
		return getUUID(name, true);
	}
	
	void update(UUID playeruuid, String name, String address);
	
	/**
	 * For every cached element, remove the specified address
	 * if it is listed. <br>
	 * <br>
	 * 
	 * @param address - the address for which to remove cached listings
	 */
	void clearCachedIp(String address);
	
	boolean uuidExists(UUID uuid);
	
	boolean hasIp(UUID playeruuid, String ip);
	
	GeoIpInfo lookupIp(final String address) throws NoGeoIpException;
	
	void loadAll(ResultSet data);
	
}
