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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import space.arim.bans.ArimBans;
import space.arim.bans.api.exception.FetcherException;
import space.arim.bans.api.exception.HttpStatusException;
import space.arim.bans.api.exception.MissingCacheException;
import space.arim.bans.api.exception.NoGeoIpException;
import space.arim.bans.api.exception.PlayerNotFoundException;
import space.arim.bans.api.exception.RateLimitException;
import space.arim.bans.api.util.FetcherUtil;
import space.arim.bans.api.util.GeoIpInfo;
import space.arim.bans.internal.sql.SqlQuery;
import space.arim.registry.RegistryPriority;

public class Resolver implements ResolverMaster {
	
	private final ArimBans center;
	
	private ConcurrentHashMap<UUID, List<String>> ips = new ConcurrentHashMap<UUID, List<String>>();
	private ConcurrentHashMap<UUID, String> uuids = new ConcurrentHashMap<UUID, String>();

	private boolean internalFetcher = true;
	private boolean ashconFetcher = true;
	private boolean mojangFetcher = true;
	private boolean ipStack = false;
	private String ipStackKey = "nokey";
	private boolean freeGeoIp = true;
	private boolean ipApi = true;
	
	public Resolver(ArimBans center) {
		this.center = center;
	}

	@Override
	public void loadAll(ResultSet data) {
		try {
			while (data.next()) {
				UUID uuid;
				try {
					uuid = UUID.fromString(data.getString("uuid"));
					uuids.put(uuid, data.getString("name"));
					ips.put(uuid, internaliseIps(data.getString("iplist")));
				} catch (IllegalArgumentException ex) {
					center.logError(ex);
				}
			}
		} catch (SQLException ex) {
			center.logError(ex);
		}
	}
	
	private String externaliseIps(List<String> iplist) {
		if (iplist.isEmpty()) {
			return "<empty>";
		}
		String list = null;
		for (String s : iplist) {
			if (list != null) {
				list = list.concat(",").concat(s);
			} else {
				list = s;
			}
		}
		return list;
	}

	private List<String> internaliseIps(String input) {
		return input.equalsIgnoreCase("<empty>") ? new ArrayList<String>() : new ArrayList<String>(Arrays.asList(input.split(",")));
	}

	@Override
	public List<String> getIps(UUID playeruuid) {
		Objects.requireNonNull(playeruuid, "UUID must not be null!");
		if (ips.containsKey(playeruuid)) {
			return ips.get(playeruuid);
		}
		return new ArrayList<String>();
	}
	
	@Override
	public List<UUID> getPlayers(String address) {
		Objects.requireNonNull(address, "Address must not be null!");
		List<UUID> applicable = new ArrayList<UUID>();
		ips.forEach((uuid, ips) -> {
			if (ips.contains(address)) {
				applicable.add(uuid);
			}
		});
		return applicable;
	}
	
	@Override
	public String getName(UUID playeruuid) throws MissingCacheException {
		if (uuids.containsKey(playeruuid)) {
			return uuids.get(playeruuid);
		}
		throw new MissingCacheException(playeruuid);
	}
	
	@Override
	public UUID getUUID(String name) throws MissingCacheException {
		for (UUID id : uuids.keySet()) {
			if (uuids.get(id).equalsIgnoreCase(name)) {
				return id;
			}
		}
		throw new MissingCacheException("Player " + name + " does not exist!");
	}
	
	@Override
	public void update(UUID playeruuid, String name, String ip) {
		if (uuids.containsKey(playeruuid)) {
			if (!uuids.get(playeruuid).equalsIgnoreCase(name) || !ips.get(playeruuid).contains(ip)) {
				center.async(() -> {
					if (!uuids.get(playeruuid).equalsIgnoreCase(name)) {
						uuids.replace(playeruuid, name);
						center.sql().executeQuery(SqlQuery.Query.UPDATE_NAME_FOR_UUID.eval(center.sql().mode()), name, System.currentTimeMillis(), playeruuid.toString());
					}
					if (!ips.get(playeruuid).contains(ip) && ip != null) {
						List<String> list = ips.get(playeruuid);
						list.add(ip);
						ips.put(playeruuid, list);
						center.sql().executeQuery(SqlQuery.Query.UPDATE_IPS_FOR_UUID.eval(center.sql().mode()), externaliseIps(list), System.currentTimeMillis(), playeruuid.toString());
					}
				});
			}
		} else {
			center.async(() -> {
				ArrayList<String> list = new ArrayList<String>();
				if (ip != null) {
					list.add(ip);
				}
				uuids.put(playeruuid, name);
				ips.put(playeruuid, list);
				center.sql().executeQuery(SqlQuery.Query.INSERT_CACHE.eval(center.sql().mode()), playeruuid.toString(), name, externaliseIps(list), System.currentTimeMillis(), System.currentTimeMillis());
			});
		}
	}
	
	@Override
	public boolean uuidExists(UUID uuid) {
		return uuids.containsKey(uuid);
	}

	@Override
	public boolean hasIp(UUID playeruuid, String ip) {
		if (ips.containsKey(playeruuid)) {
			if (ips.get(playeruuid).contains(ip)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public GeoIpInfo lookupIp(final String address) throws NoGeoIpException {
		if (ipStack) {
			try {
				return FetcherUtil.ipStack(address, ipStackKey);
			} catch (FetcherException | RateLimitException | HttpStatusException ex) {
				center.logError(ex);
			}
		}
		if (freeGeoIp) {
			try {
				return FetcherUtil.freeGeoIp(address);
			} catch (FetcherException| RateLimitException | HttpStatusException ex) {
				center.logError(ex);
			}
		}
		if (ipApi) {
			try {
				return FetcherUtil.ipApi(address);
			} catch (FetcherException| RateLimitException | HttpStatusException ex) {
				center.logError(ex);
			}
		}
		throw new NoGeoIpException(address);
	}
	
	@Override
	public UUID resolveName(String name, boolean query) throws PlayerNotFoundException {
		try {
			return getUUID(name);
		} catch (MissingCacheException ex) {}
		if (internalFetcher) {
			try {
				UUID uuid2 = center.environment().uuidFromName(name);
				update(uuid2, name, null);
				return uuid2;
			} catch (PlayerNotFoundException ex) {}
		}
		if (query && ashconFetcher) {
			try {
				UUID uuid3 = FetcherUtil.ashconApi(name);
				update(uuid3, name, null);
				return uuid3;
			} catch (FetcherException | HttpStatusException ex) {}
		}
		if (query && mojangFetcher) {
			try {
				UUID uuid4 = FetcherUtil.mojangApi(name);
				update(uuid4, name, null);
				return uuid4;
			} catch (FetcherException | HttpStatusException ex) {}
		}
		throw new PlayerNotFoundException(name);
	}
	
	@Override
	public String resolveUUID(UUID uuid, boolean query) throws PlayerNotFoundException {
		try {
			return getName(uuid);
		} catch (MissingCacheException ex) {}
		if (internalFetcher) {
			try {
				String name2 = center.environment().nameFromUUID(uuid);
				update(uuid, name2, null);
				return name2;
			} catch (PlayerNotFoundException ex) {}
		}
		if (query && ashconFetcher) {
			try {
				String name3 = FetcherUtil.ashconApi(uuid);
				update(uuid, name3, null);
				return name3;
			} catch (FetcherException | HttpStatusException ex) {}
		}
		if (query && mojangFetcher) {
			try {
				String name4 = FetcherUtil.mojangApi(uuid);
				update(uuid, name4, null);
				return name4;
			} catch (FetcherException | HttpStatusException ex) {}
		}
		throw new PlayerNotFoundException(uuid);
	}

	@Override
	public String getName() {
		return center.getName();
	}
	
	@Override
	public String getAuthor() {
		return center.getAuthor();
	}

	@Override
	public String getVersion() {
		return center.getVersion();
	}

	@Override
	public byte getPriority() {
		return RegistryPriority.HIGHER;
	}
	
	@Override
	public void refreshConfig(boolean fromFile) {
		internalFetcher = center.config().getConfigBoolean("fetchers.uuids.internal");
		ashconFetcher = center.config().getConfigBoolean("fetchers.uuids.ashcon");
		mojangFetcher = center.config().getConfigBoolean("fetchers.uuids.mojang");
		ipStack = center.config().getConfigBoolean("fetchers.ips.ipstack.enabled");
		ipStackKey = center.config().getConfigString("fetchers.ips.ipstack.key");
		freeGeoIp = center.config().getConfigBoolean("fetchers.ips.freegeoip");
		ipApi = center.config().getConfigBoolean("fetchers.ips.ipapi");
	}

}
