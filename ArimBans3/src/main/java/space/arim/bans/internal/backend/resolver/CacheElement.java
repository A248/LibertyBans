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
import space.arim.bans.internal.sql.SqlQuery;

public class CacheElement {
	
	private static final String EMPTY_IPLIST_STRING = "<empty_iplist>";
	
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
	
	private List<String> parseIpList(String iplist) {
		if (iplist.equals(EMPTY_IPLIST_STRING)) {
			return null;
		}
		return new ArrayList<String>(Arrays.asList(iplist.split(",")));
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
	
	SqlQuery setName(String name) {
		this.name = name;
		this.updateName = System.currentTimeMillis();
		return new SqlQuery(SqlQuery.Query.UPDATE_NAME_FOR_UUID);
	}
	
	void addIp(String address) {
		if (iplist == null) {
			iplist = new ArrayList<String>(Arrays.asList(address));
		} else {
			iplist.add(address);
		}
		updateIplist = System.currentTimeMillis();
	}
	
	void removeIp(String address) {
		if (iplist != null) {
			if (iplist.remove(address)) {
				if (iplist.isEmpty()) {
					iplist = null;
				}
				updateIplist = System.currentTimeMillis();
			}
		}
	}
	
}
