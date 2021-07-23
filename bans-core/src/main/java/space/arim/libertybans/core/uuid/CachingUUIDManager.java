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

import java.net.InetAddress;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.checkerframework.checker.nullness.qual.NonNull;
import space.arim.libertybans.core.config.SqlConfig;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.api.util.web.RemoteApiResult;
import space.arim.api.util.web.RemoteNameUUIDApi;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.env.EnvUserResolver;

@Singleton
public final class CachingUUIDManager implements UUIDManager {

	private final Configs configs;
	private final FactoryOfTheFuture futuresFactory;
	private final EnvUserResolver envResolver;
	private final QueryingImpl queryingImpl;

	private Cache<@NonNull String, @NonNull UUID> nameToUuidCache;
	private Cache<@NonNull UUID, @NonNull String> uuidToNameCache;
	private NameValidator nameValidator;

	@Inject
	public CachingUUIDManager(Configs configs, FactoryOfTheFuture futuresFactory,
							  Provider<InternalDatabase> dbProvider, EnvUserResolver envResolver) {
		this(configs, futuresFactory, envResolver, new QueryingImpl(dbProvider));
	}

	CachingUUIDManager(Configs configs, FactoryOfTheFuture futuresFactory,
					   EnvUserResolver envResolver, QueryingImpl queryingImpl) {
		this.configs = configs;
		this.futuresFactory = futuresFactory;
		this.envResolver = envResolver;
		this.queryingImpl = queryingImpl;
	}

	@Override
	public void startup() {
		SqlConfig.MuteCaching muteCaching = configs.getSqlConfig().muteCaching();
		Duration expiration = Duration.ofSeconds(muteCaching.expirationTimeSeconds());
		var semantic = muteCaching.expirationSemantic();
		nameToUuidCache = createCache(expiration, semantic);
		uuidToNameCache = createCache(expiration, semantic);
		nameValidator = uuidResolution().nameValidator();
	}

	private static <K, V> Cache<@NonNull K, @NonNull V> createCache(Duration expiration,
																	SqlConfig.MuteCaching.ExpirationSemantic semantic) {
		switch (semantic) {
		case EXPIRE_AFTER_ACCESS:
			return Caffeine.newBuilder().expireAfterAccess(expiration).build();
		case EXPIRE_AFTER_WRITE:
			return Caffeine.newBuilder().expireAfterWrite(expiration).build();
		default:
			throw new IllegalStateException("Unknown expiration semantic " + semantic);
		}
	}

	@Override
	public void restart() {
		startup();
	}

	@Override
	public void shutdown() { }
	
	@Override
	public void addCache(UUID uuid, String name) {
		nameToUuidCache.put(name.toLowerCase(Locale.ROOT), uuid);
		uuidToNameCache.put(uuid, name);
	}
	
	private <T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory.completedFuture(value);
	}

	private UUIDResolutionConfig uuidResolution() {
		return configs.getMainConfig().uuidResolution();
	}

	/*
	 * UUID resolution works as follows:
	 * 
	 * 1. Check caches
	 * 2. Check online players
	 * 3. Check own database
	 * 4. If online server, check Mojang API and third party web APIs where configured.
	 * If offline server and exact name provided, compute offline UUID.
	 */
	
	@Override
	public CentralisedFuture<Optional<UUID>> lookupUUID(String name) {
		return lookupUUIDExactOrNot(name, false);
	}

	@Override
	public CentralisedFuture<Optional<UUID>> lookupUUIDFromExactName(String name) {
		return lookupUUIDExactOrNot(name, true);
	}

	private CentralisedFuture<Optional<UUID>> lookupUUIDExactOrNot(String name, boolean exact) {
		if (!nameValidator.validateNameArgument(name)) {
			return completedFuture(Optional.empty());
		}
		UUID cachedResolve = nameToUuidCache.getIfPresent(name.toLowerCase(Locale.ROOT));
		if (cachedResolve != null) {
			return completedFuture(Optional.of(cachedResolve));
		}
		boolean canComputeOffline = exact && uuidResolution().serverType() == ServerType.OFFLINE;
		return lookupUUIDUncached(name, canComputeOffline).thenApply((optExternalUuid) -> {
			if (optExternalUuid.isPresent()) {
				addCache(optExternalUuid.get(), name);
			}
			return optExternalUuid;
		});
	}

	private CentralisedFuture<Optional<UUID>> lookupUUIDUncached(String name, boolean canComputeOffline) {
		Optional<UUID> envResolve = envResolver.lookupUUID(name);
		if (envResolve.isPresent()) {
			return completedFuture(envResolve);
		}
		return queryingImpl.resolve(name).thenCompose((queriedUuid) -> {
			if (queriedUuid != null) {
				return completedFuture(Optional.of(queriedUuid));
			}
			if (canComputeOffline) {
				// Offline server and exact lookup
				UUID offlineUuid = OfflineUUID.computeOfflineUuid(name);
				return completedFuture(Optional.of(offlineUuid));
			}
			// Online or mixed mode server, or inexact lookup
			return webLookup((remoteApi) -> remoteApi.lookupUUID(name));
		});
	}

	@Override
	public CentralisedFuture<Optional<String>> lookupName(UUID uuid) {
		String cachedResolve = uuidToNameCache.getIfPresent(uuid);
		if (cachedResolve != null) {
			return completedFuture(Optional.of(cachedResolve));
		}
		return lookupNameUncached(uuid).thenApply((optExternalName) -> {
			if (optExternalName.isPresent()) {
				addCache(uuid, optExternalName.get());
			}
			return optExternalName;
		});
	}

	private CentralisedFuture<Optional<String>> lookupNameUncached(UUID uuid) {
		Optional<String> envResolve = envResolver.lookupName(uuid);
		if (envResolve.isPresent()) {
			return completedFuture(envResolve);
		}
		return queryingImpl.resolve(uuid).thenCompose((queriedName) -> {
			if (queriedName != null) {
				return completedFuture(Optional.of(queriedName));
			}
			return webLookup((remoteApi) -> remoteApi.lookupName(uuid));
		});
	}
	
	private <T> CompletableFuture<Optional<T>> webLookup(Function<RemoteNameUUIDApi, CompletableFuture<RemoteApiResult<T>>> resultFunction) {
		UUIDResolutionConfig uuidResolution = uuidResolution();
		if (uuidResolution.serverType() != ServerType.ONLINE) {
			return futuresFactory.completedFuture(Optional.empty());
		}
		return uuidResolution.remoteApis().lookup(resultFunction).thenApply(Optional::ofNullable);
	}
	
	// Other lookups
	
	@Override
	public CentralisedFuture<NetworkAddress> lookupAddress(String name) {
		if (!nameValidator.validateNameArgument(name)) {
			return completedFuture(null);
		}
		Optional<InetAddress> quickResolve = envResolver.lookupAddress(name);
		if (quickResolve.isPresent()) {
			return completedFuture(NetworkAddress.of(quickResolve.get()));
		}
		return queryingImpl.resolveAddress(name);
	}

	@Override
	public CentralisedFuture<Optional<UUIDAndAddress>> lookupPlayer(String name) {
		if (!nameValidator.validateNameArgument(name)) {
			return completedFuture(Optional.empty());
		}
		Optional<UUIDAndAddress> quickResolve = envResolver.lookupPlayer(name);
		if (quickResolve.isPresent()) {
			return futuresFactory.completedFuture(quickResolve);
		}
		return queryingImpl.resolvePlayer(name).thenApply(Optional::ofNullable);
	}
	
}
