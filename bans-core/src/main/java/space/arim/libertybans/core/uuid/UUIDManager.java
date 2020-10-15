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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.uuidvault.api.CollectiveUUIDResolver;
import space.arim.uuidvault.api.UUIDVault;
import space.arim.uuidvault.api.UUIDVaultRegistration;

import space.arim.api.util.web.RemoteApiResult;
import space.arim.api.util.web.RemoteNameUUIDApi;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.Part;

public class UUIDManager implements Part {

	final LibertyBansCore core;
	
	final Cache<UUID, String> fastCache = Caffeine.newBuilder().expireAfterAccess(20L, TimeUnit.SECONDS).build();
	private final ResolverImpl resolverImpl;
	
	private volatile UUIDVaultStuff uuidStuff;
	
	private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]*+");
	
	public UUIDManager(LibertyBansCore core) {
		this.core = core;
		resolverImpl = new ResolverImpl(this);
	}
	
	private static class UUIDVaultStuff {

		final UUIDVaultRegistration registration;
		final CollectiveUUIDResolver collectiveResolver;
		
		UUIDVaultStuff(UUIDVaultRegistration registration, CollectiveUUIDResolver collectiveResolver) {
			this.registration = registration;
			this.collectiveResolver = collectiveResolver;
		}
		
	}
	
	@Override
	public void startup() {
		Class<?> pluginClass = core.getEnvironment().getPlatformHandle().getImplementingPluginInfo().getPlugin().getClass();
		UUIDVault uuidVault = core.getEnvironment().getUUIDVault();
		UUIDVaultRegistration registration = uuidVault.register(resolverImpl, pluginClass, (byte) 0, "LibertyBans");
		CollectiveUUIDResolver collectiveResolver = uuidVault.createCollectiveResolverIgnoring(registration);
		uuidStuff = new UUIDVaultStuff(registration, collectiveResolver);
	}
	
	@Override
	public void restart() {
		
	}
	
	@Override
	public void shutdown() {
		UUIDVaultStuff uuidStuff = this.uuidStuff;
		core.getEnvironment().getUUIDVault().unregister(uuidStuff.registration);
	}
	
	public void addCache(UUID uuid, String name) {
		fastCache.put(uuid, name);
	}
	
	private <T> CentralisedFuture<T> completedFuture(T value) {
		return core.getFuturesFactory().completedFuture(value);
	}
	
	public boolean validateNameArgument(String name) {
		return validateNameArgument0(name);
	}
	
	static boolean validateNameArgument0(String name) {
		return name.length() <= 16 && VALID_NAME_PATTERN.matcher(name).matches();
	}
	
	/*
	 * UUID resolution works as follows:
	 * 
	 * 1. Check online players
	 * 2. Check fast cache using own resolver
	 * 3. Check own database using own resolver
	 * 4. Check other resolvers through UUIDVault.
	 * 5. If online server, check Mojang API and third party web APIs where configured.
	 * If offline server, calculate UUID if possible. If mixed server, stop.
	 * 
	 * If 4 or 5 found an answer, add it to the fast cache.
	 * 
	 */
	
	public CentralisedFuture<UUID> fullLookupUUID(final String name) {
		UUID quickResolve;
		UUIDVault uuidVault = core.getEnvironment().getUUIDVault();
		if (uuidVault.mustCallNativeResolutionSync()) {
			quickResolve = core.getFuturesFactory().supplySync(() -> uuidVault.resolveNatively(name)).join();
		} else {
			quickResolve = uuidVault.resolveNatively(name);
		}
		if (quickResolve != null || (quickResolve = resolverImpl.resolveImmediately(name)) != null) {
			return completedFuture(quickResolve);
		}
		return resolverImpl.resolve(name).thenCompose((uuid1) -> {
			if (uuid1 != null) {
				return completedFuture(uuid1);
			}
			return externalLookup(name).thenApply((uuid2) -> {
				if (uuid2 != null) {
					fastCache.put(uuid2, name);
				}
				return uuid2;
			});
		});
	}
	
	private CompletableFuture<UUID> externalLookup(final String name) {
		CompletableFuture<UUID> result = uuidStuff.collectiveResolver.resolve(name);
		return webLookup(result, (remoteApi) -> remoteApi.lookupUUID(name));
	}

	public CentralisedFuture<String> fullLookupName(final UUID uuid) {
		String quickResolve;
		UUIDVault uuidVault = core.getEnvironment().getUUIDVault();
		if (uuidVault.mustCallNativeResolutionSync()) {
			quickResolve = core.getFuturesFactory().supplySync(() -> uuidVault.resolveNatively(uuid)).join();
		} else {
			quickResolve = uuidVault.resolveNatively(uuid);
		}
		if (quickResolve != null || (quickResolve = resolverImpl.resolveImmediately(uuid)) != null) {
			return completedFuture(quickResolve);
		}
		return resolverImpl.resolve(uuid).thenCompose((name1) -> {
			if (name1 != null) {
				return completedFuture(name1);
			}
			return externalLookup(uuid).thenApply((name2) -> {
				if (name2 != null) {
					fastCache.put(uuid, name2);
				}
				return name2;
			});
		});
	}
	
	private CompletableFuture<String> externalLookup(final UUID uuid) {
		CompletableFuture<String> result = uuidStuff.collectiveResolver.resolve(uuid);
		return webLookup(result, (remoteApi) -> remoteApi.lookupName(uuid));
	}
	
	private <T> CompletableFuture<T> webLookup(CompletableFuture<T> result,
			Function<RemoteNameUUIDApi, CompletableFuture<RemoteApiResult<T>>> resultFunction) {
		UUIDResolutionConfig uuidResolution = core.getMainConfig().uuidResolution();
		if (uuidResolution.serverType() != ServerType.ONLINE) {
			return result;
		}
		return uuidResolution.remoteApis().lookup(result, resultFunction);
	}
	
}
