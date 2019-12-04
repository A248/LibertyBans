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

import space.arim.bans.api.util.ToolsUtil;
import space.arim.bans.internal.sql.SqlQuery;

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
	
	SqlQuery getInsertionQuery(UUID uuid) {
		return new SqlQuery(SqlQuery.Query.INSERT_CACHE, uuid.toString().replace("-", ""), name, externaliseIpList(iplist), updateName, updateIplist);
	}
	
	private static List<String> parseIpList(String iplist) {
		if (iplist.equals(EMPTY_IPLIST_STRING)) {
			return null;
		}
		return new ArrayList<String>(Arrays.asList(iplist.split(",")));
	}
	
	private static String externaliseIpList(List<String> iplist) {
		if (iplist == null || iplist.isEmpty()) {
			return EMPTY_IPLIST_STRING;
		}
		return ToolsUtil.concat(iplist, ',');
	}
	
	String getName() {
		return name;
	}
	
	List<String> getIps() {
		return (iplist != null) ? Collections.unmodifiableList(iplist) : Collections.emptyList();
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
	
	boolean hasIp(String address) {
		return (iplist == null || iplist.contains(address));
	}
	
	SqlQuery removeIp(UUID uuid, String address) {
		if (iplist != null) {
			if (iplist.remove(address)) {
				if (iplist.isEmpty()) {
					iplist = null;
				}
				updateIplist = System.currentTimeMillis();
			}
		}
		return new SqlQuery(SqlQuery.Query.UPDATE_IPS_FOR_UUID, externaliseIpList(iplist), updateIplist, uuid.toString().replace("-", ""));
	}
	
	SqlQuery setNameAndAddIp(UUID uuid, String name, String address) {
		if (iplist == null) {
			iplist = new ArrayList<String>(Arrays.asList(address));
		} else {
			iplist.add(address);
		}
		updateIplist = System.currentTimeMillis();
		this.name = name;
		updateName = System.currentTimeMillis();
		return new SqlQuery(SqlQuery.Query.UPDATE_CACHE_FOR_UUID, name, externaliseIpList(iplist), updateName, updateIplist, uuid.toString().replace("-", ""));
	}
	
}
