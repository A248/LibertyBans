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

import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.uuidvault.api.CollectiveUUIDResolver;
import space.arim.uuidvault.api.UUIDVault;
import space.arim.uuidvault.api.UUIDVaultRegistration;

import space.arim.api.configure.SingleKeyValueTransformer;
import space.arim.api.configure.ValueTransformer;
import space.arim.api.util.web.RemoteApiResult;
import space.arim.api.util.web.RemoteApiResult.ResultType;
import space.arim.api.util.web.RemoteNameUUIDApi;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.Part;

public class UUIDMaster implements Part {

	final LibertyBansCore core;
	
	final Cache<UUID, String> fastCache = Caffeine.newBuilder().expireAfterAccess(20L, TimeUnit.SECONDS).build();
	private final ResolverImpl resolverImpl;
	
	private volatile UUIDVaultStuff uuidStuff;
	
	private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]*+");
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	public UUIDMaster(LibertyBansCore core) {
		this.core = core;
		resolverImpl = new ResolverImpl(this);
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
		ServerType serverType = getServerType();
		if (serverType != ServerType.ONLINE) {
			return result;
		}
		for (RemoteApi remoteApi : getRemoteApis()) {
			result = result.thenCompose((name) -> {
				if (name != null) {
					return completedFuture(name);
				}
				return resultFunction.apply(remoteApi.getRemote()).thenApply((remoteApiResult) -> {
					if (remoteApiResult.getResultType() != ResultType.FOUND) {
						Exception ex = remoteApiResult.getException();
						if (ex == null) {
							logger.warn("Request for name to remote web API {} failed", remoteApi);
						} else {
							logger.warn("Request for name to remote web API {} failed", remoteApi, ex);
						}
						return null;
					}
					return remoteApiResult.getValue();
				});
			});
		}
		return result;
	}
	
	private ServerType getServerType() {
		return core.getConfigs().getConfig().getObject("player-uuid-resolution.server-type", ServerType.class);
	}
	
	private List<RemoteApi> getRemoteApis() {
		return core.getConfigs().getConfig().getList("player-uuid-resolution.web-api-resolvers", RemoteApi.class);
	}
	
	public static List<? extends ValueTransformer> createValueTransformers() {
		ValueTransformer serverTypeTransformer = SingleKeyValueTransformer.create("player-uuid-resolution.server-type",
				(value) -> {

					if (value instanceof String) {
						try {
							return ServerType.valueOf((String) value);
						} catch (IllegalArgumentException ignored) {
						}
					}
					logger.warn("Invalid option for server-type: {}", value);
					return null;
				});
		ValueTransformer webApiResolversTransformer = SingleKeyValueTransformer.create("player-uuid-resolution.web-api-resolvers",
				(value) -> {

					if (!(value instanceof List<?>)) {
						logger.warn("Invalid list for web-api-resolvers: {}", value);
						return null;
					}
					@SuppressWarnings("unchecked")
					List<Object> asList = (List<Object>) value;
					for (ListIterator<Object> it = asList.listIterator(); it.hasNext();) {
						Object element = it.next();
						if (element instanceof String) {
							RemoteApi remoteApi = RemoteApi.nullableValueOf((String) element);
							if (remoteApi != null) {
								it.set(remoteApi);
								continue;
							}
						}
						logger.warn("Invalid list element in web-api-resolvers: {}", element);
						return null;
					}
					/*
					 * By now, value will be a List where each element is an instance of RemoteApi
					 */
					return value;
				});
		return List.of(serverTypeTransformer, webApiResolversTransformer);
	}
	
	enum ServerType {
		
		ONLINE,
		OFFLINE,
		MIXED
		
	}
	
}
