/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

import jakarta.inject.Provider;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.alts.AddressWhitelist;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.SqlConfig;
import space.arim.libertybans.core.config.SqlConfig.MuteCaching.ExpirationSemantic;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

abstract class BaseMuteCache implements MuteCache {

	private final Configs configs;
	private final FactoryOfTheFuture futuresFactory;
	private final Provider<QueryExecutor> queryExecutor;
	private final AddressWhitelist addressWhitelist;
	private final PunishmentSelector selector;

	BaseMuteCache(Configs configs, FactoryOfTheFuture futuresFactory, Provider<QueryExecutor> queryExecutor,
				  AddressWhitelist addressWhitelist, PunishmentSelector selector) {
		this.configs = configs;
        this.futuresFactory = futuresFactory;
        this.queryExecutor = queryExecutor;
        this.addressWhitelist = addressWhitelist;
        this.selector = selector;
	}

	// Setup

	void installCache(BiConsumer<Duration, ExpirationSemantic> settingsConsumer) {
		SqlConfig sqlConfig = configs.getSqlConfig();
		SqlConfig.MuteCaching muteCaching = sqlConfig.muteCaching();

		Duration expirationTime = Duration.ofSeconds(muteCaching.expirationTimeSeconds());
		ExpirationSemantic expirationSemantic;
		if (sqlConfig.synchronization().enabled()) {
			// If synchronization is enabled, always use expire-after-write semantics
			expirationSemantic = ExpirationSemantic.EXPIRE_AFTER_WRITE;
		} else {
			expirationSemantic = muteCaching.expirationSemantic();
		}
		settingsConsumer.accept(expirationTime, expirationSemantic);
	}

	// Retrieval

	final CentralisedFuture<Optional<Punishment>> queryPunishment(MuteCacheKey key) {
		return selector
				.getApplicablePunishment(key.uuid(), key.address(), PunishmentType.MUTE)
				.toCompletableFuture()
				.thenCompose(optPunishment -> {
					Victim victim;
					if (optPunishment.isEmpty() || (victim = optPunishment.get().getVictim()) instanceof PlayerVictim
							|| !configs.getMainConfig().enforcement().ipWhitelist().enable()) {
						return futuresFactory.completedFuture(optPunishment);
					}
					NetworkAddress victimAddress;
					if (victim instanceof AddressVictim addressVictim) {
						victimAddress = addressVictim.getAddress();
					} else if (victim instanceof CompositeVictim compositeVictim) {
						if (compositeVictim.getUUID().equals(key.uuid())) {
							return futuresFactory.completedFuture(optPunishment);
						}
						victimAddress = compositeVictim.getAddress();
					} else {
						throw MiscUtil.unknownVictimType(victim.getType());
					}
					return queryExecutor.get().query(SQLFunction.readOnly(context -> {
						return addressWhitelist.isWhitelisted(context, victimAddress);
					})).thenApply(whitelisted -> {
						if (whitelisted) {
							return Optional.empty();
						} else {
							return optPunishment;
						}
					});
				});
	}

	// Management

	@Override
	public void clearCachedMute(Punishment punishment) {
		if (punishment.getType() != PunishmentType.MUTE) {
			throw new IllegalArgumentException("Cannot clear cached mute of a punishment which is not a mute");
		}
		clearCachedMuteIf(punishment::equals);
	}

	@Override
	public void clearCachedMute(long id) {
		clearCachedMuteIf((punishment) -> punishment.getIdentifier() == id);
	}

	abstract void clearCachedMuteIf(Predicate<Punishment> removeIfMatches);

	@Override
	public final void setCachedMute(UUID uuid, NetworkAddress address, Punishment punishment) {
		if (punishment.getType() != PunishmentType.MUTE) {
			throw new IllegalArgumentException("Cannot set cached mute to a punishment which is not a mute");
		}
		setCachedMute(new MuteCacheKey(uuid, address), punishment);
	}

	abstract void setCachedMute(MuteCacheKey cacheKey, Punishment mute);

}
