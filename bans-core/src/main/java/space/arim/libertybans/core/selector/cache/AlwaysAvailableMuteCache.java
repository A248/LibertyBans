/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

package space.arim.libertybans.core.selector.cache;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.DelayCalculators;
import space.arim.omnibus.util.concurrent.EnhancedExecutor;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ScheduledTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * A mute cache which guarantees that, for online players, there is always a cached value available.
 * For offline players no such guarantee is made. <br>
 * <br>
 * This is used for platforms such as Bukkit and Sponge where mute information must be available
 * synchronously.
 *
 */
@Singleton
public final class AlwaysAvailableMuteCache extends BaseMuteCache {

	private final FactoryOfTheFuture futuresFactory;
	private final EnhancedExecutor enhancedExecutor;
	private final EnvUserResolver envUserResolver;
	private final InternalFormatter formatter;
	private final Time time;

	private volatile Cache cache;

	static final long GRACE_PERIOD_NANOS = TimeUnit.MINUTES.toNanos(4);
	static final Duration PURGE_TASK_INTERVAL = Duration.ofMinutes(3L);

	@Inject
	public AlwaysAvailableMuteCache(Configs configs, FactoryOfTheFuture futuresFactory,
									PunishmentSelector selector, EnhancedExecutor enhancedExecutor,
									EnvUserResolver envUserResolver, InternalFormatter formatter, Time time) {
		super(configs, selector);
		this.futuresFactory = futuresFactory;
		this.enhancedExecutor = enhancedExecutor;
		this.envUserResolver = envUserResolver;
		this.formatter = formatter;
		this.time = time;
	}

	@Override
	public void startup() {
		installCache((expirationTime, expirationSemantic) -> {
			Cache cache = new Cache(new ConcurrentHashMap<>(), expirationTime);
			cache.startPurgeTask();
			this.cache = cache;
		});
	}

	@Override
	public void restart() {
		installCache((expirationTime, expirationSemantic) -> {
			Cache oldCache = this.cache;
			oldCache.stopPurgeTask();
			Cache newCache = new Cache(oldCache.map, expirationTime);
			newCache.startPurgeTask();
			this.cache = newCache;
		});
	}

	@Override
	public void shutdown() {
		cache.stopPurgeTask();
	}

	private long nanoTime() {
		return time.arbitraryNanoTime();
	}

	private CentralisedFuture<MuteAndMessage> queryPunishmentAndMessage(MuteCacheKey key) {
		return queryPunishment(key).thenCompose((optMute) -> {
			if (optMute.isEmpty()) {
				return futuresFactory.completedFuture(null);
			}
			return formatMessage(optMute.get());
		});
	}

	private CentralisedFuture<MuteAndMessage> formatMessage(Punishment mute) {
		return formatter.getPunishmentMessage(mute)
				.thenApply((message) -> new MuteAndMessage(mute, message));
	}

	@Override
	public CentralisedFuture<Optional<Punishment>> getCachedMute(UUID uuid, NetworkAddress address) {
		return cacheRequest(new MuteCacheKey(uuid, address))
				.thenApply((opt) -> opt.map(MuteAndMessage::mute));
	}

	@Override
	public CentralisedFuture<Optional<Component>> getCachedMuteMessage(UUID uuid, NetworkAddress address) {
		return cacheRequest(new MuteCacheKey(uuid, address))
				.thenApply((opt) -> opt.map(MuteAndMessage::message));
	}

	private CentralisedFuture<Optional<MuteAndMessage>> cacheRequest(MuteCacheKey cacheKey) {
		Cache cache = this.cache;
		Entry cacheEntry = cache.map.computeIfPresent(cacheKey, (key, entry) -> {

			MuteAndMessage currentValue = entry.currentValue;
			long lastUpdated = entry.lastUpdated;
			CentralisedFuture<MuteAndMessage> nextValue = entry.nextValue;

			// If the next value is ready, replace the current value with it
			if (nextValue != null && nextValue.isDone()) {
				currentValue = nextValue.join();
				nextValue = null;
			}
			// If the current value is old, begin to compute a new value for it
			final long currentTime = nanoTime();
			long updatedAgo = currentTime - lastUpdated;
			if (updatedAgo >= cache.expirationTimeNanos) {
				nextValue = queryPunishmentAndMessage(key);
				lastUpdated = currentTime;
			}
			return new Entry(currentValue, lastUpdated, nextValue);
		});
		if (cacheEntry == null) {
			// The player is offline. This should only happen through an API request.
			// In that case, we query the database and skip caching
			return queryPunishmentAndMessage(cacheKey).thenApply(Optional::ofNullable);
		}
		return futuresFactory.completedFuture(Optional.ofNullable(cacheEntry.currentValue));
	}

	@Override
	void clearCachedMuteIf(Predicate<Punishment> removeIfMatches) {
		ConcurrentHashMap<MuteCacheKey, Entry> map = cache.map;

		for (Map.Entry<MuteCacheKey, Entry> mapEntry : map.entrySet()) {
			MuteCacheKey key = mapEntry.getKey();
			Entry entry = mapEntry.getValue();

			// Replace the current value with NULL if it matches the predicate
			// However, perform the operation atomically with respect to entry updates
			MuteAndMessage currentValue;
			while ((currentValue = entry.currentValue) != null && removeIfMatches.test(currentValue.mute())) {
				Entry newEntry = new Entry(null, entry.lastUpdated, entry.nextValue);
				// Compare-and-swap the old entry with the new one
				if (map.replace(key, entry, newEntry)) {
					// Success
					break;
				}
				entry = map.get(key);
				if (entry == null) {
					// Player logged off
					break;
				}
			}
		}
	}

	@Override
	public CentralisedFuture<?> cacheOnLogin(UUID uuid, NetworkAddress address) {
		final long currentTime = nanoTime();
		// There might be an existing entry if the player rejoins before periodic invalidation
		Entry entry = cache.map.compute(new MuteCacheKey(uuid, address), (key, existingEntry) -> {
			if (existingEntry == null) {
				// Most common
				return new Entry(null, currentTime, queryPunishmentAndMessage(key));
			}
			// Use the existing entry; refresh it if necessary
			MuteAndMessage currentValue = existingEntry.currentValue;
			CentralisedFuture<MuteAndMessage> nextValue = existingEntry.nextValue;
			// If the next value is ready, replace the current value with it
			if (nextValue != null && nextValue.isDone()) {
				currentValue = nextValue.join();
				nextValue = null;
			}
			long originallyUpdatedAgo = currentTime - existingEntry.lastUpdated;
			if (originallyUpdatedAgo >= cache.expirationTimeNanos) {
				nextValue = queryPunishmentAndMessage(key);
			}
			// Always update lastUpdated, to prevent periodic invalidation
			// But subtract 1 to signal to ourselves outside the lambda
			return new Entry(currentValue, currentTime - 1, nextValue);
		});
		if (entry.lastUpdated == currentTime) {
			// Wait for our newly-entered computation
			return entry.nextValue;
		}
		// There's no need to wait: we have an existing entry
		return futuresFactory.completedFuture(null);
	}

	@Override
	void setCachedMute(MuteCacheKey cacheKey, Punishment mute) {
		cache.map.compute(cacheKey, (key, entry) -> {
			// If there is no existing entry, store the new entry
			if (entry == null) {
				return new Entry(null, nanoTime(), formatMessage(mute));
			}

			// Update the old entry, if needed, with the new mute
			MuteAndMessage currentValue = entry.currentValue;
			long lastUpdated = entry.lastUpdated;
			CentralisedFuture<MuteAndMessage> nextValue = entry.nextValue;

			if (nextValue != null) {
				// If the next value is ready, replace the current value with it
				if (nextValue.isDone()) {
					currentValue = nextValue.join();
					nextValue = null;
				} else {
					// There is a next value in-progress but not yet ready
					// Keep it, as it will be more accurate in case there are multiple applicable mutes
					return entry;
				}
			}
			// If there is no current mute, or the new mute will expire less soon, use the new mute
			if (currentValue == null || mute.getEndDate().isAfter(currentValue.mute().getEndDate())) {
				nextValue = formatMessage(mute);
				lastUpdated = nanoTime();
			}
			return new Entry(currentValue, lastUpdated, nextValue);
		});
	}

	private final class Cache {

		private final ConcurrentHashMap<MuteCacheKey, Entry> map;
		private final long expirationTimeNanos;
		private ScheduledTask purgeTask;

		private Cache(ConcurrentHashMap<MuteCacheKey, Entry> map, Duration expirationTime) {
			this.map = map;
			this.expirationTimeNanos = expirationTime.toNanos();
		}

		private void startPurgeTask() {
			purgeTask = enhancedExecutor.scheduleRepeating(() -> {

				Collection<CentralisedFuture<Void>> removalFutures = new ArrayList<>(map.size() + 10);
				long currentTime = nanoTime();

				for (Map.Entry<MuteCacheKey, Entry> mapEntry : map.entrySet()) {
					MuteCacheKey key = mapEntry.getKey();
					var thisFuture = envUserResolver.lookupName(key.uuid()).thenAccept((nameIfOnline) -> {
						if (nameIfOnline.isPresent()) {
							// The player is online
							return;
						}
						Entry entry = mapEntry.getValue();
						if (currentTime - entry.lastUpdated <= GRACE_PERIOD_NANOS) {
							/*
							Not enough time has passed. This allows a grace period in which cache entries may exist
							despite the player is not logged in.

							This solves the login process conundrum which occurs when the client is between
							the login event and join event (AsyncPlayerPreLoginEvent and PlayerJoinEvent on Bukkit).
							We reasonably assume the time between login event and join event < 4 minutes.
							 */
							return;
						}
						// The player is offline and the grace period has passed
						// IMPORTANT: This relies on the exact Entry instance for concurrent correctness
						map.remove(key, entry);
					});
					removalFutures.add(thisFuture);
				}
				// Wait for all futures to complete
				futuresFactory.allOf(removalFutures).join();

			}, PURGE_TASK_INTERVAL, DelayCalculators.fixedDelay());
		}

		private void stopPurgeTask() {
			purgeTask.cancel();
		}
	}

	private record Entry(@Nullable MuteAndMessage currentValue, long lastUpdated,
						 @Nullable CentralisedFuture<MuteAndMessage> nextValue) { }

}
