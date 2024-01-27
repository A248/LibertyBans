/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.core.uuid;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.checkerframework.checker.nullness.qual.NonNull;
import space.arim.api.util.web.RemoteApiResult;
import space.arim.api.util.web.RemoteNameUUIDApi;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Singleton
public final class CachingUUIDManager implements UUIDManager {

	private final Configs configs;
	private final FactoryOfTheFuture futuresFactory;
	private final EnvUserResolver envResolver;
	private final QueryingImpl queryingImpl;
	private final NameValidator nameValidator;
	private final Time time;

	private Cache<@NonNull String, @NonNull UUID> nameToUuidCache;
	private Cache<@NonNull UUID, @NonNull String> uuidToNameCache;

	@Inject
	public CachingUUIDManager(Configs configs, FactoryOfTheFuture futuresFactory,
							  Provider<InternalDatabase> dbProvider, EnvUserResolver envResolver,
							  NameValidator nameValidator, Time time) {
		this(configs, futuresFactory, envResolver, new QueryingImpl(dbProvider), nameValidator, time);
	}

	CachingUUIDManager(Configs configs, FactoryOfTheFuture futuresFactory,
					   EnvUserResolver envResolver, QueryingImpl queryingImpl,
					   NameValidator nameValidator, Time time) {
		this.configs = configs;
		this.futuresFactory = futuresFactory;
		this.envResolver = envResolver;
		this.queryingImpl = queryingImpl;
		this.nameValidator = nameValidator;
		this.time = time;
	}

	@Override
	public void startup() {
		nameToUuidCache = Caffeine.newBuilder()
				.ticker(time.toCaffeineTicker())
				.expireAfterAccess(Duration.ofMinutes(15L))
				.build();
		uuidToNameCache = Caffeine.newBuilder()
				.ticker(time.toCaffeineTicker())
				.expireAfterAccess(Duration.ofMinutes(15L))
				.build();
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
	 * 1. Check cache
	 * 2. Check online players
	 * 3. Check own database
	 * 4. If online server, check Mojang API and third party web APIs where configured.
	 * If offline server and exact name provided, compute offline uuid.
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
		if (exact && uuidResolution().serverType() == ServerType.OFFLINE) {
			// Offline server and exact lookup
			UUID offlineUuid = OfflineUUID.computeOfflineUuid(name);
			addCache(offlineUuid, name);
			return completedFuture(Optional.of(offlineUuid));
		}
		return lookupUUIDUncached(name).thenApply((optExternalUuid) -> {
			if (optExternalUuid.isPresent()) {
				addCache(optExternalUuid.get(), name);
			}
			return optExternalUuid;
		});
	}

	private CentralisedFuture<Optional<UUID>> lookupUUIDUncached(String name) {
		// 1. Resolve by environment
		return envResolver.lookupUUID(name).thenCompose((envResolve) -> {
			if (envResolve.isPresent()) {
				return completedFuture(envResolve);
			}
			// 2. Resolve by database query
			return queryingImpl.resolve(name).thenCompose((queriedUuid) -> {
				if (queriedUuid != null) {
					return completedFuture(Optional.of(queriedUuid));
				}
				// 3. Resolve by web API
				if (nameValidator.isVanillaName(name)) {
					return webLookup((remoteApi) -> remoteApi.lookupUUID(name));
				} else {
					return completedFuture(Optional.empty());
				}
			});
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
		// 1. Resolve by environment
		return envResolver.lookupName(uuid).thenCompose((envResolve) -> {
			if (envResolve.isPresent()) {
				return completedFuture(envResolve);
			}
			// 2. Resolve by database query
			return queryingImpl.resolve(uuid).thenCompose((queriedName) -> {
				if (queriedName != null) {
					return completedFuture(Optional.of(queriedName));
				}
				// 3. Resolve by web API
				if (nameValidator.isVanillaUUID(uuid)) {
					return webLookup((remoteApi) -> remoteApi.lookupName(uuid));
				} else {
					return completedFuture(Optional.empty());
				}
			});
		});
	}

	private <T> CompletableFuture<Optional<T>> webLookup(Function<RemoteNameUUIDApi, CompletableFuture<RemoteApiResult<T>>> resultFunction) {
		UUIDResolutionConfig uuidResolution = uuidResolution();
		if (uuidResolution.serverType() != ServerType.ONLINE) {
			return completedFuture(Optional.empty());
		}
		return uuidResolution.remoteApis().lookup(resultFunction).thenApply(Optional::ofNullable);
	}

	// Other lookups

	@Override
	public CentralisedFuture<NetworkAddress> lookupAddress(String name) {
		if (!nameValidator.validateNameArgument(name)) {
			return completedFuture(null);
		}
		return envResolver.lookupAddress(name).thenCompose((envResolve) -> {
			if (envResolve.isPresent()) {
				return completedFuture(NetworkAddress.of(envResolve.get()));
			}
			return queryingImpl.resolveAddress(name);
		});
	}

	@Override
	public CentralisedFuture<Optional<UUIDAndAddress>> lookupPlayer(String name) {
		if (!nameValidator.validateNameArgument(name)) {
			return completedFuture(Optional.empty());
		}
		return envResolver.lookupPlayer(name).thenCompose((envResolve) -> {
			if (envResolve.isPresent()) {
				return completedFuture(envResolve);
			}
			return queryingImpl.resolvePlayer(name).thenApply(Optional::ofNullable);
		});
	}

	@Override
	public CentralisedFuture<Optional<NetworkAddress>> lookupLastAddress(UUID uuid) {
		return envResolver.lookupCurrentAddress(uuid).thenCompose((envResolve) -> {
			if (envResolve.isPresent()) {
				return completedFuture(envResolve.map(NetworkAddress::of));
			}
			return queryingImpl.resolveLastAddress(uuid).thenApply(Optional::ofNullable);
		});
	}

}
