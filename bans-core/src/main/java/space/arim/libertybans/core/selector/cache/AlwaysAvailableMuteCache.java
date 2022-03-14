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
import space.arim.libertybans.core.config.SqlConfig;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
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
	private final InternalFormatter formatter;
	private final Time time;

	private volatile Cache cache;

	@Inject
	public AlwaysAvailableMuteCache(Configs configs, FactoryOfTheFuture futuresFactory,
									PunishmentSelector selector, InternalFormatter formatter, Time time) {
		super(configs, selector);
		this.futuresFactory = futuresFactory;
		this.formatter = formatter;
		this.time = time;
	}

	@Override
	void installCache(Duration expirationTime, SqlConfig.MuteCaching.ExpirationSemantic expirationSemantic) {
		cache = new Cache(expirationTime);
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
		return cacheRequestTo(uuid, address, MuteAndMessage::mute);
	}

	@Override
	public CentralisedFuture<Optional<Component>> getCachedMuteMessage(UUID uuid, NetworkAddress address) {
		return cacheRequestTo(uuid, address, MuteAndMessage::message);
	}

	private <T> CentralisedFuture<Optional<T>> cacheRequestTo(UUID uuid, NetworkAddress address,
															  Function<MuteAndMessage, T> toWhich) {
		return cacheRequest(new MuteCacheKey(uuid, address))
				.thenApply((muteAndMessage) -> muteAndMessage.map(toWhich));
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
		// Do nothing - the mute will automatically expire in due time
	}

	@Override
	public CentralisedFuture<?> cacheOnLogin(UUID uuid, NetworkAddress address) {
		MuteCacheKey key = new MuteCacheKey(uuid, address);
		return queryPunishmentAndMessage(key).thenAccept((value) -> {
			// Add to cache
			Entry previousEntry = cache.map.put(key, new Entry(value, nanoTime(), null));
			// Sanity check
			if (previousEntry != null) {
				// Very bad if this is happening
				Duration updatedAgo = Duration.ofNanos(nanoTime() - previousEntry.lastUpdated);
				throw new IllegalStateException(
						"Found an existing mute cache entry for player " + uuid + ". " +
								"Maybe the player is already logged in? Last updated " + updatedAgo);
			}
		});
	}

	@Override
	public void uncacheOnQuit(UUID uuid, NetworkAddress address) {
		// Clean removal - stops any in-progress computation, too
		Entry removed = cache.map.remove(new MuteCacheKey(uuid, address));
		// Sanity check
		if (removed == null) {
			throw new IllegalStateException("Expected there to be a mute cache entry for player " + uuid);
		}
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

	private static final class Cache {

		private final ConcurrentHashMap<MuteCacheKey, Entry> map = new ConcurrentHashMap<>();
		private final long expirationTimeNanos;

		private Cache(Duration expirationTime) {
			this.expirationTimeNanos = expirationTime.toNanos();
		}
	}

	private static final class Entry {

		private final @Nullable MuteAndMessage currentValue;
		private final long lastUpdated;
		private final @Nullable CentralisedFuture<@Nullable MuteAndMessage> nextValue;

		private Entry(@Nullable MuteAndMessage currentValue, long lastUpdated,
					  @Nullable CentralisedFuture<@Nullable MuteAndMessage> nextValue) {
			this.currentValue = currentValue;
			this.lastUpdated = lastUpdated;
			this.nextValue = nextValue;
		}
	}
}
