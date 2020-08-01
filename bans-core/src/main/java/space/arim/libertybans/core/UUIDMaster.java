/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.uuidvault.api.UUIDResolver;
import space.arim.uuidvault.api.UUIDUtil;
import space.arim.uuidvault.api.UUIDVault;
import space.arim.uuidvault.api.UUIDVaultRegistration;

import space.arim.libertybans.core.database.Database;

public class UUIDMaster implements Part {

	private final LibertyBansCore core;
	
	private final Cache<UUID, String> fastCache = Caffeine.newBuilder().expireAfterAccess(20L, TimeUnit.SECONDS).build();
	private final UUIDResolver resolver;
	
	private volatile UUIDVaultRegistration uvr;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	UUIDMaster(LibertyBansCore core) {
		this.core = core;
		resolver = new ResolverImpl(core, fastCache);
	}
	
	@Override
	public void startup() {
		Class<?> pluginClass = core.getEnvironment().getPluginClass();
		uvr = UUIDVault.get().register(resolver, pluginClass, (byte) 0, "LibertyBans");
	}
	
	@Override
	public void restart() {}
	
	@Override
	public void shutdown() {
		UUIDVault.get().unregister(uvr);
	}
	
	public void addCache(UUID uuid, String name) {
		fastCache.put(uuid, name);
	}
	
	public CentralisedFuture<UUID> fullLookupUUID(String name) {
		return core.getFuturesFactory().supplySync(() -> UUIDVault.get().resolveNatively(name)).thenCompose((uuid) -> {
			if (uuid != null) {
				return CompletableFuture.completedFuture(uuid);
			}
			return UUIDVault.get().resolve(name);
		}).thenApply((uuid) -> {
			if (uuid != null) {
				fastCache.put(uuid, name);
			}
			return uuid;
		});
	}
	
	public CentralisedFuture<String> fullLookupName(UUID uuid) {
		return core.getFuturesFactory().supplySync(() -> UUIDVault.get().resolveNatively(uuid)).thenCompose((name) -> {
			if (name != null) {
				return CompletableFuture.completedFuture(name);
			}
			return UUIDVault.get().resolve(uuid);
		}).thenApply((name) -> {
			if (name != null) {
				fastCache.put(uuid, name);
			}
			return name;
		});
	}
	
	/*
	 * 
	 * UUIDResolver impl
	 * 
	 */
	
	private static class ResolverImpl implements UUIDResolver {
		
		private final LibertyBansCore core;
		private final Cache<UUID, String> fastCache;
		
		ResolverImpl(LibertyBansCore core, Cache<UUID, String> fastCache) {
			this.core = core;
			this.fastCache = fastCache;
		}
		
		@Override
		public CentralisedFuture<UUID> resolve(String name) {
			Database helper = core.getDatabase();
			return helper.selectAsync(() -> {
				try (ResultSet rs = helper.getBackend().select(
						"SELECT `uuid` FROM `libertybans_names` WHERE `name` = ? ORDER BY `updated` DESC LIMIT 1", name)) {
					if (rs.next()) {
						return UUIDUtil.fromByteArray(rs.getBytes("uuid"));
					}
				} catch (SQLException ex) {
					logger.warn("Could not resolve name {}", name, ex);
				}
				return null;
			});
		}

		@Override
		public CentralisedFuture<String> resolve(UUID uuid) {
			Database helper = core.getDatabase();
			return helper.selectAsync(() -> {
				try (ResultSet rs = helper.getBackend().select(
						"SELECT `name` FROM `libertybans_names` WHERE `uuid` = ? ORDER BY `updated` DESC LIMIT 1", UUIDUtil.toByteArray(uuid))) {
					if (rs.next()) {
						return rs.getString("name");
					}
				} catch (SQLException ex) {
					logger.warn("Could not resolve uuid {}", uuid, ex);
				}
				return null;
			});
		}

		@Override
		public UUID resolveImmediately(String name) {
			// Caffeine specifies that operations on the entry set do not refresh the expiration timer
			for (Map.Entry<UUID, String> entry : fastCache.asMap().entrySet()) {
				if (entry.getValue().equalsIgnoreCase(name)) {
					UUID uuid = entry.getKey();
					// Manual cache refresh
					fastCache.getIfPresent(uuid);
					return uuid;
				}
			}
			return null;
		}

		@Override
		public String resolveImmediately(UUID uuid) {
			return fastCache.getIfPresent(uuid);
		}
		
	}
	
}
