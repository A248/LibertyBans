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

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.slf4j.LoggerFactory;
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
import java.util.function.Predicate;

/**
 * Mute cache used for Velocity and BungeeCord which queries for mutes as needed,
 * using a layer of caching for efficiency.
 *
 */
@Singleton
public final class OnDemandMuteCache extends BaseMuteCache {

	private final FactoryOfTheFuture futuresFactory;
	private final InternalFormatter formatter;
	private final Time time;

	private volatile AsyncLoadingCache<MuteCacheKey, Optional<Punishment>> cache;

	@Inject
	public OnDemandMuteCache(Configs configs, FactoryOfTheFuture futuresFactory,
							 PunishmentSelector selector, InternalFormatter formatter, Time time) {
		super(configs, selector);
		this.futuresFactory = futuresFactory;
		this.formatter = formatter;
		this.time = time;
	}

	@Override
	void installCache(Duration expirationTime, SqlConfig.MuteCaching.ExpirationSemantic expirationSemantic) {

		Caffeine<Object, Object> builder = Caffeine.newBuilder();
		switch (expirationSemantic) {
		case EXPIRE_AFTER_ACCESS:
			builder = builder.expireAfterAccess(expirationTime);
			break;
		case EXPIRE_AFTER_WRITE:
			builder = builder.expireAfterWrite(expirationTime);
			break;
		default:
			throw new IllegalStateException("Unknown expiration semantic " + expirationSemantic);
		}
		cache = builder
				.scheduler(Scheduler.disabledScheduler())
				.ticker(time.toCaffeineTicker())
				.buildAsync((key, executor) -> {
					return queryPunishment(key).exceptionally((ex) -> {
						// If we don't catch these exceptions, Caffeine will
						LoggerFactory.getLogger(OnDemandMuteCache.class)
								.warn("Exception while computing cached mute", ex);
						return Optional.empty();
					});
				});
	}

	@Override
	void uninstallCache() {}

	@Override
	public CentralisedFuture<Optional<Punishment>> getCachedMute(UUID uuid, NetworkAddress address) {
		return cacheRequest(new MuteCacheKey(uuid, address));
	}

	@Override
	public CentralisedFuture<Optional<Component>> getCachedMuteMessage(UUID uuid, NetworkAddress address) {
		return cacheRequest(new MuteCacheKey(uuid, address)).thenCompose((optMute) -> {
			if (optMute.isEmpty()) {
				return futuresFactory.completedFuture(Optional.empty());
			}
			return formatter.getPunishmentMessage(optMute.get()).thenApply(Optional::of);
		});
	}

	private CentralisedFuture<Optional<Punishment>> cacheRequest(MuteCacheKey key) {
		var cache = this.cache;
		var muteFuture = cache.get(key);
		// We need to check if the cached mute is expired, and if so, re-compute the mute.
		if (muteFuture.isDone()) {
			Punishment mute = muteFuture.join().orElse(null);
			if (mute != null && mute.isExpired(time.toJdkClock())) {
				// Remove from the cache and re-request
				cache.asMap().remove(key, muteFuture);
				return cacheRequest(key);
			}
		}
		return futuresFactory.copyFuture(muteFuture);
	}

	@Override
	void clearCachedMuteIf(Predicate<Punishment> removeIfMatches) {
		cache.synchronous().asMap().values().removeIf((mute) -> {
			return mute.isPresent() && removeIfMatches.test(mute.get());
		});
	}

	@Override
	public CentralisedFuture<?> cacheOnLogin(UUID uuid, NetworkAddress address) {
		return futuresFactory.completedFuture(null);
	}

	@Override
	public void uncacheOnQuit(UUID uuid, NetworkAddress address) {}

	@Override
	void setCachedMute(MuteCacheKey cacheKey, Punishment mute) {
		cache.asMap().compute(cacheKey, (key, future) -> {
			if (future == null) {
				// Install the new mute
				return futuresFactory.completedFuture(Optional.of(mute));
			}
			if (!future.isDone()) {
				// Keep the existing computation:
				// it will be more accurate in case there are multiple applicable mutes
				return future;
			}
			Punishment oldMute = future.join().orElse(null);
			// If there is no current mute, or the new mute will expire less soon, use the new mute
			if (oldMute == null || mute.getEndDate().isAfter(oldMute.getEndDate())) {
				return futuresFactory.completedFuture(Optional.of(mute));
			}
			return future;
		});
	}
}
