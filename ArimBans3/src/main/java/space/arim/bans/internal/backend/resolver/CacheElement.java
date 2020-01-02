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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import space.arim.bans.internal.sql.BasicQuery;

public class CacheElement {
	
	static final String EMPTY_IPLIST_STRING = "<empty_iplist>";
	
	private final UUID uuid;
	private final String name;
	private final String iplist;
	private final long updateName;
	private final long updateIplist;
	
	CacheElement(UUID uuid, String name, String iplist, long updateName, long updateIplist) {
		this.uuid = uuid;
		this.name = name;
		this.iplist = iplist;
		this.updateName = updateName;
		this.updateIplist = updateIplist;
	}
	
	CacheElement(UUID uuid, String name, String iplist) {
		this(uuid, name, iplist, System.currentTimeMillis(), System.currentTimeMillis());
	}
	
	private String updateIps(String address) {
		return iplist == null ? address : iplist + "," + address;
	}
	
	BasicQuery insert() {
		return new BasicQuery(BasicQuery.PreQuery.INSERT_CACHE, uuid, name, iplist, updateName, updateIplist);
	}
	
	BasicQuery setNameAndAddIp(String name, String address) {
		long current = System.currentTimeMillis();
		return new BasicQuery(BasicQuery.PreQuery.UPDATE_CACHE_FOR_UUID, name, updateIps(address), current, current, uuid);
	}
	
	BasicQuery setName(String name) {
		return new BasicQuery(BasicQuery.PreQuery.UPDATE_NAME_FOR_UUID, name, System.currentTimeMillis(), uuid);
	}
	
	public BasicQuery addIp(String address) {
		return new BasicQuery(BasicQuery.PreQuery.UPDATE_IPS_FOR_UUID, updateIps(address), System.currentTimeMillis(), uuid);
	}
	
	UUID uuid() {
		return uuid;
	}
	
	String getName() {
		return name;
	}
	
	public Set<String> getIps() {
		if (iplist == null) {
			return Collections.emptySet();
		}
		Set<String> ips = new HashSet<String>();
		for (String ip : iplist.split(",")) {
			ips.add(ip);
		}
		return ips;
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
	
	/**
	 * Checks whether this CacheElement has ever had a specific IP address
	 * 
	 * @param address the IP address
	 * @return true if the address is in the list
	 */
	public boolean hasIp(String address) {
		return iplist != null && iplist.contains(address);
	}
	
}
