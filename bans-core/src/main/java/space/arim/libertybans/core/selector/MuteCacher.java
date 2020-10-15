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
package space.arim.libertybans.core.selector;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.NonNull;

import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.LibertyBansCore;

public class MuteCacher {
	
	private final LibertyBansCore core;
	
	/**
	 * The mute cache. Optionals are used because Caffeine understands 'null' to mean 'remove this from the cache'.
	 * 
	 */
	private final AsyncLoadingCache<MuteCacheKey, Optional<Punishment>> muteCache;
	
	public MuteCacher(LibertyBansCore core) {
		this.core = core;

		AsyncCacheLoader<MuteCacheKey, Optional<Punishment>> cacheLoader = new AsyncCacheLoader<>() {
			@Override
			public @NonNull CentralisedFuture<Optional<Punishment>> asyncLoad(@NonNull MuteCacheKey key,
					@NonNull Executor executor) {
				return core.getSelector().getApplicableMute(key.uuid, key.address)
						.thenApply(Optional::ofNullable)
						.exceptionally((ex) -> {
							LoggerFactory.getLogger(getClass()).warn("Exception while computing cached mute", ex);
							return Optional.empty();
						});
			}
		};
		muteCache = Caffeine.newBuilder().expireAfterAccess(4L, TimeUnit.MINUTES).initialCapacity(32)
				.scheduler(Scheduler.systemScheduler()).buildAsync(cacheLoader);
	}

	public CentralisedFuture<Punishment> getCachedMute(UUID uuid, NetworkAddress address) {
		return (CentralisedFuture<Punishment>) muteCache.get(new MuteCacheKey(uuid, address))
				.thenApply((optPunishment) -> {
					Punishment punishment = optPunishment.orElse(null);
					if (punishment == null || punishment.isExpired()) {
						return null;
					}
					return punishment;
				});
	}
	
	public void setCachedMute(UUID uuid, NetworkAddress address, Punishment punishment) {
		if (punishment.getType() != PunishmentType.MUTE) {
			throw new IllegalArgumentException("Cannot set cached mute to a punishment which is not a mute");
		}
		muteCache.put(new MuteCacheKey(uuid, address), core.getFuturesFactory().completedFuture(Optional.of(punishment)));
	}
	
	private void clearCachedMuteIf(Predicate<Punishment> filter) {
		/*
		 * Any matching entries must be fully removed so that they may be recalculated afresh,
		 * and not merely set to an empty Optional, the reason being there may be multiple
		 * applicable mutes for the UUID/address combination.
		 */
		muteCache.synchronous().asMap().values().removeIf((optPunishment) -> {
			return optPunishment.isPresent() && filter.test(optPunishment.get());
		});
	}
	
	public void clearCachedMute(Punishment punishment) {
		if (punishment.getType() != PunishmentType.MUTE) {
			throw new IllegalArgumentException("Cannot clear cached mute of a punishment which is not a mute");
		}
		clearCachedMuteIf(punishment::equals);
	}

	public void clearCachedMute(int id) {
		clearCachedMuteIf((punishment) -> punishment.getID() == id);
	}
	
	public void clearCachedMute(Victim victim) {
		clearCachedMuteIf((punishment) -> punishment.getVictim().equals(victim));
	}
	
	private static class MuteCacheKey {
		
		final UUID uuid;
		final NetworkAddress address;
		
		MuteCacheKey(UUID uuid, NetworkAddress address) {
			this.uuid = uuid;
			this.address = address;
		}

		@Override
		public String toString() {
			return "MuteCacheKey [uuid=" + uuid + ", address=" + address + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + uuid.hashCode();
			result = prime * result + address.hashCode();
			return result;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof MuteCacheKey)) {
				return false;
			}
			MuteCacheKey other = (MuteCacheKey) object;
			return uuid.equals(other.uuid) && address.equals(other.address);
		}
		
	}
	
}
