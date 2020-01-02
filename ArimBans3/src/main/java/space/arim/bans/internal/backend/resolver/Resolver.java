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
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import space.arim.bans.ArimBans;
import space.arim.bans.api.exception.MissingCacheException;
import space.arim.bans.api.exception.NoGeoIpException;
import space.arim.bans.internal.sql.SelectionQuery;

import space.arim.universal.registry.RegistryPriority;
import space.arim.universal.util.exception.HttpStatusException;

import space.arim.api.uuid.PlayerNotFoundException;
import space.arim.api.util.minecraft.MinecraftUtil;
import space.arim.api.util.web.FetcherException;
import space.arim.api.util.web.FetcherUtil;
import space.arim.api.util.web.GeoIpInfo;
import space.arim.api.util.web.RateLimitException;

public class Resolver implements ResolverMaster {
	
	private final ArimBans center;
	
	private final Object lock = new Object();

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
	public CacheElement singleFromResultSet(ResultSet data) throws SQLException {
		return new CacheElement(MinecraftUtil.expandAndParseUUID(data.getString("uuid")), data.getString("name"), data.getString("iplist"), data.getLong("update_name"), data.getLong("update_iplist"));
	}
	
	@Override
	public Set<CacheElement> setFromResultSet(ResultSet data) throws SQLException {
		Set<CacheElement> cache = new HashSet<CacheElement>();
		while (data.next()) {
			cache.add(singleFromResultSet(data));
		}
		return cache;
	}
	
	@Override
	public Set<String> getIps(UUID playeruuid) throws MissingCacheException {
		Objects.requireNonNull(playeruuid, "UUID must not be null!");
		try (ResultSet data = center.sql().execute(SelectionQuery.create("cache").addCondition("uuid", playeruuid))[0]){
			if (data.next()) {
				return singleFromResultSet(data).getIps();
			}
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
		return Collections.emptySet();
	}
	
	@Override
	public Set<UUID> getPlayers(String address) {
		Objects.requireNonNull(address, "Address must not be null!");
		Set<CacheElement> cache;
		try (ResultSet data = center.sql().execute(SelectionQuery.create("cache"))[0]){
			cache = setFromResultSet(data);
		} catch (SQLException ex) {
			center.logs().logError(ex);
			return Collections.emptySet();
		}
		if (cache.isEmpty()) {
			return Collections.emptySet();
		}
		Set<UUID> uuids = new HashSet<UUID>();
		cache.forEach((element) -> {
			if (element.hasIp(address)) {
				uuids.add(element.uuid());
			}
		});
		return uuids;
	}
	
	@Override
	public String getName(UUID playeruuid) throws MissingCacheException {
		try (ResultSet data = center.sql().execute(SelectionQuery.create("cache").addCondition("uuid", playeruuid))[0]) {
			if (data.next()) {
				return singleFromResultSet(data).getName();
			}
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
		throw new MissingCacheException(playeruuid);
	}
	
	@Override
	public UUID getUUID(String name, boolean ignoreCase) throws MissingCacheException {
		Objects.requireNonNull(name, "Name must not be null!");
		Set<CacheElement> cache;
		try (ResultSet data = center.sql().execute(SelectionQuery.create("cache"))[0]){
			cache = setFromResultSet(data);
		} catch (SQLException ex) {
			center.logs().logError(ex);
			throw new MissingCacheException(name);
		}
		for (CacheElement element : cache) {
			if (element.hasName(name, ignoreCase)) {
				return element.uuid();
			}
		}
		throw new MissingCacheException(name);
	}
	
	private void directUpdate(CacheElement element, String name, String address) {
		final boolean updateName = !element.hasName(name);
		final boolean updateIplist = !element.hasIp(address);
		center.sql().execute((updateName && updateIplist) ? element.setNameAndAddIp(name, address) : updateName ? element.setName(name) : element.addIp(address));
	}
	
	private void directInsert(CacheElement element) {
		center.sql().execute(element.insert());
	}
	
	@Override
	public void update(UUID uuid, String name, String address) {
		synchronized (lock) {
			try (ResultSet data = center.sql().execute(SelectionQuery.create("cache").addCondition("uuid", uuid))[0]) {
				if (data.next()) {
					directUpdate(singleFromResultSet(data), name, address);
				} else {
					directInsert(new CacheElement(uuid, name, address));
				}
			} catch (SQLException ex) {
				center.logs().logError(ex);
			}
		}
	}

	@Override
	public boolean hasIp(UUID playeruuid, String address) {
		try (ResultSet data = center.sql().execute(SelectionQuery.create("cache").addCondition("uuid", playeruuid))[0]) {
			return data.next() && singleFromResultSet(data).hasIp(address);
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
		return false;
	}
	
	@Override
	public GeoIpInfo lookupIp(final String address) throws NoGeoIpException {
		if (ipStack) {
			try {
				return FetcherUtil.ipStack(address, ipStackKey);
			} catch (FetcherException | RateLimitException | HttpStatusException ex) {
				center.logs().logError(ex);
			}
		}
		if (freeGeoIp) {
			try {
				return FetcherUtil.freeGeoIp(address);
			} catch (FetcherException | RateLimitException | HttpStatusException ex) {
				center.logs().logError(ex);
			}
		}
		if (ipApi) {
			try {
				return FetcherUtil.ipApi(address);
			} catch (FetcherException | RateLimitException | HttpStatusException ex) {
				center.logs().logError(ex);
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
	public void refreshConfig(boolean first) {
		internalFetcher = center.config().getConfigBoolean("fetchers.uuids.internal");
		ashconFetcher = center.config().getConfigBoolean("fetchers.uuids.ashcon");
		mojangFetcher = center.config().getConfigBoolean("fetchers.uuids.mojang");
		ipStack = center.config().getConfigBoolean("fetchers.geoip.ipStack.enable");
		ipStackKey = center.config().getConfigString("fetchers.geoip.ipStack.key");
		freeGeoIp = center.config().getConfigBoolean("fetchers.geoip.freeGeoIp");
		ipApi = center.config().getConfigBoolean("fetchers.geoip.ipApi");
	}

}
