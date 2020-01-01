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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import space.arim.bans.internal.sql.SqlQuery;

import space.arim.api.util.StringsUtil;

public class CacheElement {
	
	static final String EMPTY_IPLIST_STRING = "<empty_iplist>";
	
	private String name;
	private List<String> iplist;
	private long updateName;
	private long updateIplist;
	
	CacheElement(String name, String iplist, long updateName, long updateIplist) {
		this.name = name;
		this.iplist = parseIpList(iplist);
		this.updateName = updateName;
		this.updateIplist = updateIplist;
	}
	
	CacheElement(String name, String iplist) {
		this(name, iplist, System.currentTimeMillis(), System.currentTimeMillis());
	}
	
	SqlQuery insert(UUID uuid) {
		return new SqlQuery(SqlQuery.Query.INSERT_CACHE, uuid.toString().replace("-", ""), name, externaliseIpList(iplist), updateName, updateIplist);
	}
	
	private static List<String> parseIpList(String iplist) {
		if (iplist == null || iplist.equals(EMPTY_IPLIST_STRING)) {
			return null;
		}
		return new ArrayList<String>(Arrays.asList(iplist.split(",")));
	}
	
	private static String externaliseIpList(List<String> iplist) {
		if (iplist == null || iplist.isEmpty()) {
			return EMPTY_IPLIST_STRING;
		}
		return StringsUtil.concat(iplist, ',');
	}
	
	String getName() {
		return name;
	}
	
	List<String> getIps() {
		return iplist != null ? Collections.unmodifiableList(iplist) : Collections.emptyList();
	}
	
	long getNameUpdate() {
		return updateName;
	}
	
	long getIplistUpdate() {
		return updateIplist;
	}
	
	SqlQuery setName(UUID uuid, String name) {
		this.name = name;
		updateName = System.currentTimeMillis();
		return new SqlQuery(SqlQuery.Query.UPDATE_NAME_FOR_UUID, name, updateName, uuid.toString().replace("-", ""));
	}
	
	SqlQuery addIp(UUID uuid, String address) {
		if (iplist == null) {
			iplist = new ArrayList<String>(Arrays.asList(address));
		} else {
			iplist.add(address);
		}
		updateIplist = System.currentTimeMillis();
		return new SqlQuery(SqlQuery.Query.UPDATE_IPS_FOR_UUID, externaliseIpList(iplist), updateIplist, uuid.toString().replace("-", ""));
	}
	
	/**
	 * Checks if this CacheElement's player name is the input string
	 * 
	 * @param name the playername
	 * @param ignoreCase whether to check case sensitivity
	 * @return true if and only if the internal name equals the playername
	 */
	boolean hasName(String name, boolean ignoreCase) {
		return getName().equals(name) || ignoreCase && getName().equalsIgnoreCase(name);
	}
	
	/**
	 * Equivalent to calling <code>hasName(name, true)</code>
	 * 
	 * @param name the playername to check for
	 * @return true if and only if the internal name equals the playername
	 */
	boolean hasName(String name) {
		return hasName(name, true);
	}
	
	boolean hasIp(String address) {
		return iplist != null && iplist.contains(address);
	}
	
	/**
	 * Caller should check {@link #hasIp(String)} before this method.
	 * 
	 * @param uuid the related uuid
	 * @param address the IP address
	 * @return a SqlQuery
	 */
	SqlQuery removeIp(UUID uuid, String address) {
		if (iplist.remove(address)) {
			if (iplist.isEmpty()) {
				iplist = null;
			}
			updateIplist = System.currentTimeMillis();
		}
		return new SqlQuery(SqlQuery.Query.UPDATE_IPS_FOR_UUID, externaliseIpList(iplist), updateIplist, uuid.toString().replace("-", ""));
	}
	
	SqlQuery setNameAndAddIp(UUID uuid, String newName, String address) {
		if (iplist == null) {
			iplist = new ArrayList<String>(Arrays.asList(address));
		} else {
			iplist.add(address);
		}
		updateIplist = System.currentTimeMillis();
		name = newName;
		updateName = System.currentTimeMillis();
		return new SqlQuery(SqlQuery.Query.UPDATE_CACHE_FOR_UUID, newName, externaliseIpList(iplist), updateName, updateIplist, uuid.toString().replace("-", ""));
	}
	
}
