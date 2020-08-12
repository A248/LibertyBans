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
package space.arim.libertybans.core.uuid;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.uuidvault.api.CollectiveUUIDResolver;
import space.arim.uuidvault.api.UUIDResolver;
import space.arim.uuidvault.api.UUIDUtil;
import space.arim.uuidvault.api.UUIDVault;
import space.arim.uuidvault.api.UUIDVaultRegistration;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.Part;
import space.arim.libertybans.core.database.Database;

public class UUIDMaster implements Part {

	final LibertyBansCore core;
	
	final Cache<UUID, String> fastCache = Caffeine.newBuilder().expireAfterAccess(20L, TimeUnit.SECONDS).build();
	private final UUIDResolver resolver;
	
	private volatile UUIDVaultStuff uuidStuff;
	
	public UUIDMaster(LibertyBansCore core) {
		this.core = core;
		resolver = new ResolverImpl();
	}
	
	@Override
	public void startup() {
		Class<?> pluginClass = core.getEnvironment().getPlatformHandle().getImplementingPluginInfo().getPlugin().getClass();
		UUIDVault uuidVault = UUIDVault.get();
		UUIDVaultRegistration registration = uuidVault.register(resolver, pluginClass, (byte) 0, "LibertyBans");
		CollectiveUUIDResolver resolver = uuidVault.createCollectiveResolverIgnoring(registration);
		uuidStuff = new UUIDVaultStuff(registration, resolver);
	}
	
	@Override
	public void restart() {
		
	}
	
	@Override
	public void shutdown() {
		UUIDVaultStuff uuidStuff = this.uuidStuff;
		UUIDVault.get().unregister(uuidStuff.registration);
	}
	
	public void addCache(UUID uuid, String name) {
		fastCache.put(uuid, name);
	}
	
	public CentralisedFuture<UUID> fullLookupUUID(final String name) {
		return core.getFuturesFactory().supplySync(() -> UUIDVault.get().resolveNatively(name)).thenCompose((uuid1) -> {
			if (uuid1 != null) {
				return CompletableFuture.completedFuture(uuid1);
			}
			return uuidStuff.resolver.resolve(name).thenApply((uuid2) -> {
				if (uuid2 != null) {
					fastCache.put(uuid2, name);
				}
				return uuid2;
			});
		});
	}
	
	public CentralisedFuture<String> fullLookupName(final UUID uuid) {
		return core.getFuturesFactory().supplySync(() -> UUIDVault.get().resolveNatively(uuid)).thenCompose((name1) -> {
			if (name1 != null) {
				return CompletableFuture.completedFuture(name1);
			}
			return uuidStuff.resolver.resolve(uuid).thenApply((name2) -> {
				if (name2 != null) {
					fastCache.put(uuid, name2);
				}
				return name2;
			});
		});
	}
	
	/*
	 * 
	 * UUIDResolver impl
	 * 
	 */
	
	private class ResolverImpl implements UUIDResolver {
		
		ResolverImpl() {

		}
		
		@Override
		public CentralisedFuture<UUID> resolve(String name) {
			Database helper = core.getDatabase();
			return helper.selectAsync(() -> {
				return helper.jdbCaesar().query(
						"SELECT `uuid` FROM `libertybans_names` WHERE `name` = ? ORDER BY `updated` DESC LIMIT 1")
						.params(name)
						.singleResult((resultSet) -> {
							return UUIDUtil.fromByteArray(resultSet.getBytes("uuid"));
						}).onError(() -> null).execute();
			});
		}

		@Override
		public CentralisedFuture<String> resolve(UUID uuid) {
			Database helper = core.getDatabase();
			return helper.selectAsync(() -> {
				return helper.jdbCaesar().query(
						"SELECT `name` FROM `libertybans_names` WHERE `uuid` = ? ORDER BY `updated` DESC LIMIT 1")
						.params(uuid)
						.singleResult((resultSet) -> {
							return resultSet.getString("name");
						}).onError(() -> null).execute();
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
