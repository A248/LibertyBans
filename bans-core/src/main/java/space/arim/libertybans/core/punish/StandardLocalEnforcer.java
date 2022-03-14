/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.core.punish;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.EnforcementOptions.Broadcasting;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.config.AdditionsSection;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.RemovalsSection;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.env.AdditionalUUIDTargetMatcher;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.ExactTargetMatcher;
import space.arim.libertybans.core.env.TargetMatcher;
import space.arim.libertybans.core.env.UUIDTargetMatcher;
import space.arim.libertybans.core.selector.AddressStrictness;
import space.arim.libertybans.core.selector.cache.MuteCache;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static space.arim.libertybans.core.schema.tables.Addresses.ADDRESSES;
import static space.arim.libertybans.core.schema.tables.StrictLinks.STRICT_LINKS;

@Singleton
public final class StandardLocalEnforcer implements LocalEnforcer {

	private final Configs configs;
	private final FactoryOfTheFuture futuresFactory;
	private final Provider<QueryExecutor> queryExecutor;
	private final PunishmentSelector selector;
	private final InternalFormatter formatter;
	private final MuteCache muteCache;
	private final EnvEnforcer<?> envEnforcer;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public StandardLocalEnforcer(Configs configs, FactoryOfTheFuture futuresFactory,
								 Provider<QueryExecutor> queryExecutor, PunishmentSelector selector,
								 InternalFormatter formatter, EnvEnforcer<?> envEnforcer, MuteCache muteCache) {
		this.configs = configs;
		this.futuresFactory = futuresFactory;
		this.queryExecutor = queryExecutor;
		this.selector = selector;
		this.formatter = formatter;
		this.envEnforcer = envEnforcer;
		this.muteCache = muteCache;
	}

	@Override
	public CentralisedFuture<Void> enforceWithoutSynchronization(Punishment punishment,
																 EnforcementOpts enforcementOptions) {
		assert enforcementOptions.enforcement() != EnforcementOptions.Enforcement.NONE : "Handled elsewhere";

		AdditionsSection.PunishmentAddition section = configs.getMessagesConfig().additions().forType(punishment.getType());

		var arrestsAndNotices = new Parameterized<>(envEnforcer).enforceArrestsAndNotices(punishment);
		if (enforcementOptions.broadcasting() == Broadcasting.NONE) {
			return arrestsAndNotices;
		}
		return arrestsAndNotices.thenCompose((ignore) -> {
			return formatter.formatWithPunishment(
					enforcementOptions.replaceTargetArgument(section.successNotification()),
					punishment
			);
		}).thenAccept((notification) -> {
			boolean silent = enforcementOptions.broadcasting() == Broadcasting.SILENT;
			envEnforcer.sendToThoseWithPermission(
					new PunishmentPermission(
							punishment.getType(), Mode.DO
					).notifyPermission(silent),
					notification
			);
		});
	}

	@Override
	public CentralisedFuture<Void> unenforceWithoutSynchronization(Punishment punishment,
																   EnforcementOpts enforcementOptions) {
		assert enforcementOptions.enforcement() != EnforcementOptions.Enforcement.NONE : "Handled elsewhere";

		if (punishment.getType() == PunishmentType.MUTE) {
			muteCache.clearCachedMute(punishment);
		}
		if (enforcementOptions.broadcasting() == EnforcementOptions.Broadcasting.NONE) {
			return completedFuture(null);
		}
		CentralisedFuture<Component> futureNotify;
		{
			RemovalsSection.PunishmentRemoval section = configs.getMessagesConfig().removals().forType(punishment.getType());
			ComponentText successNotification = enforcementOptions.replaceTargetArgument(section.successNotification());

			Optional<Operator> unOperator = enforcementOptions.unOperator();
			if (unOperator.isEmpty()) {
				futureNotify = formatter.formatWithPunishment(successNotification, punishment);
			} else {
				futureNotify = formatter.formatWithPunishmentAndUnoperator(successNotification, punishment, unOperator.get());
			}
		}
		return futureNotify.thenAccept((notification) -> {
			boolean silent = enforcementOptions.broadcasting() == EnforcementOptions.Broadcasting.SILENT;
			envEnforcer.sendToThoseWithPermission(
					new PunishmentPermission(
							punishment.getType(), Mode.UNDO
					).notifyPermission(silent),
					notification
			);
		});
	}

	@Override
	public CentralisedFuture<Void> unenforceWithoutSynchronization(long id, PunishmentType type,
																   EnforcementOpts enforcementOptions) {
		assert enforcementOptions.enforcement() != EnforcementOptions.Enforcement.NONE : "Handled elsewhere";

		if (enforcementOptions.broadcasting() != EnforcementOptions.Broadcasting.NONE) {
			// In order to send broadcast messages we need the full punishment details
			// Therefore we need to fetch the punishment, then redirect to the other unenforce method
			logger.debug("Fetching punishment details for id {} and type {}", id, type);
			return selector.getHistoricalPunishmentByIdAndType(id, type).thenCompose((optPunishment) -> {
				if (optPunishment.isEmpty()) {
					logger.warn("Tried to unenforce non-existent punishment with id {} and type {}", id, type);
					return completedFuture(null);
				}
				Punishment punishment = optPunishment.get();
				return unenforceWithoutSynchronization(punishment, enforcementOptions);
			}).toCompletableFuture();
		}
		if (type == PunishmentType.MUTE) {
			muteCache.clearCachedMute(id);
		}
		return completedFuture(null);
	}

	// Enforcement of enacted punishments

	private static boolean shouldKick(PunishmentType type) {
		switch (type) {
		case BAN:
		case KICK:
			return true;
		case MUTE:
		case WARN:
			return false;
		default:
			throw MiscUtil.unknownType(type);
		}
	}

	private class Parameterized<P> {

		private final EnvEnforcer<P> envEnforcer;

		Parameterized(EnvEnforcer<P> envEnforcer) {
			this.envEnforcer = envEnforcer;
		}

		CentralisedFuture<Void> enforceArrestsAndNotices(Punishment punishment) {

			CentralisedFuture<Component> futureMessage = formatter.getPunishmentMessage(punishment);
			Victim victim = punishment.getVictim();
			UUID uuid;
			NetworkAddress address;

			switch (victim.getType()) {
			case PLAYER:
				uuid = ((PlayerVictim) victim).getUUID();
				return futureMessage.thenAccept((message) -> {
					envEnforcer.doForPlayerIfOnline(uuid, enforcementCallback(punishment, message));
				});
			case ADDRESS:
				address = ((AddressVictim) victim).getAddress();
				return futureMessage
						.thenCompose((message) -> matchAddressPunishment(punishment, message, address))
						.thenAccept(envEnforcer::enforceMatcher);
			case COMPOSITE:
				CompositeVictim compositeVictim = (CompositeVictim) victim;
				address = compositeVictim.getAddress();
				uuid = compositeVictim.getUUID();
				return futureMessage
						.thenCompose((message) -> matchAddressPunishment(punishment, message, address))
						.thenApply((addressMatcher) -> new AdditionalUUIDTargetMatcher<>(uuid, addressMatcher))
						.thenAccept(envEnforcer::enforceMatcher);
			default:
				throw MiscUtil.unknownVictimType(victim.getType());
			}
		}

		private Consumer<P> enforcementCallback(Punishment punishment, Component message) {
			PunishmentType type = punishment.getType();
			boolean shouldKick = shouldKick(type);
			return (player) -> {

				if (shouldKick) {
					envEnforcer.kickPlayer(player, message);
				} else {
					envEnforcer.sendMessageNoPrefix(player, message);

					/*
					 * Mute enforcement must additionally take into account the mute cache
					 */
					if (type == PunishmentType.MUTE) {
						UUID uuid = envEnforcer.getUniqueIdFor(player);
						NetworkAddress address = NetworkAddress.of(envEnforcer.getAddressFor(player));
						muteCache.setCachedMute(uuid, address, punishment);
					}
				}
			};
		}

		private CentralisedFuture<TargetMatcher<P>> matchAddressPunishment(
				Punishment punishment, Component message, NetworkAddress address) {
			CentralisedFuture<TargetMatcher<P>> futureMatcher;
			AddressStrictness strictness = configs.getMainConfig().enforcement().addressStrictness();
			switch (strictness) {
			case LENIENT:
				futureMatcher = completedFuture(
						new ExactTargetMatcher<>(address, enforcementCallback(punishment, message)));
				break;
			case NORMAL:
				futureMatcher = matchAddressPunishmentNormal(address, punishment, message);
				break;
			case STRICT:
				futureMatcher = matchAddressPunishmentStrict(address, punishment, message);
				break;
			default:
				throw MiscUtil.unknownAddressStrictness(strictness);
			}
			if (logger.isDebugEnabled()) {
				futureMatcher.thenAccept((matcher) -> {
					logger.debug("Enforcing {} address punishment with matcher {}", strictness, matcher);
				});
			}
			return futureMatcher;
		}

		private CentralisedFuture<TargetMatcher<P>> matchAddressPunishmentNormal(
				NetworkAddress address, Punishment punishment, Component message) {
			return queryExecutor.get().query(SQLFunction.readOnly((context) -> {
				return context
						.select(ADDRESSES.UUID)
						.from(ADDRESSES)
						.where(ADDRESSES.ADDRESS.eq(address))
						.fetchSet(ADDRESSES.UUID);
			})).thenApply((uuids) -> {
				return new UUIDTargetMatcher<>(uuids, enforcementCallback(punishment, message));
			});
		}

		private CentralisedFuture<TargetMatcher<P>> matchAddressPunishmentStrict(
				NetworkAddress address, Punishment punishment, Component message) {
			return queryExecutor.get().query(SQLFunction.readOnly((context) -> {
				return context
						.select(STRICT_LINKS.UUID2)
						.from(STRICT_LINKS)
						.innerJoin(ADDRESSES)
						.on(STRICT_LINKS.UUID1.eq(ADDRESSES.UUID))
						.where(ADDRESSES.ADDRESS.eq(address))
						.fetchSet(STRICT_LINKS.UUID2);
			})).thenApply((uuids) -> {
				return new UUIDTargetMatcher<>(uuids, enforcementCallback(punishment, message));
			});
		}
	}

	private <T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory.completedFuture(value);
	}

}
