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

package space.arim.libertybans.core.punish;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.api.env.annote.PlatformPlayer;
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
import space.arim.libertybans.api.select.AddressStrictness;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.PunishmentAdditionSection;
import space.arim.libertybans.core.config.RemovalsSection;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.env.AdditionalUUIDTargetMatcher;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.ExactTargetMatcher;
import space.arim.libertybans.core.env.InstanceType;
import space.arim.libertybans.core.env.TargetMatcher;
import space.arim.libertybans.core.env.UUIDTargetMatcher;
import space.arim.libertybans.core.env.message.KickPlayer;
import space.arim.libertybans.core.punish.permission.PunishmentPermission;
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
public final class StandardLocalEnforcer<@PlatformPlayer P> implements LocalEnforcer {

	private final InstanceType instanceType;
	private final Configs configs;
	private final FactoryOfTheFuture futuresFactory;
	private final Provider<QueryExecutor> queryExecutor;
	private final PunishmentSelector selector;
	private final InternalFormatter formatter;
	private final EnvEnforcer<P> envEnforcer;
	private final MuteCache muteCache;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public StandardLocalEnforcer(InstanceType instanceType, Configs configs, FactoryOfTheFuture futuresFactory,
								 Provider<QueryExecutor> queryExecutor, PunishmentSelector selector,
								 InternalFormatter formatter, EnvEnforcer<P> envEnforcer, MuteCache muteCache) {
		this.instanceType = instanceType;
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

		PunishmentAdditionSection section = configs.getMessagesConfig().additions().forType(punishment.getType());

		var arrestsAndNotices = enforceArrestsAndNotices(punishment);
		if (enforcementOptions.broadcasting() == Broadcasting.NONE) {
			return arrestsAndNotices;
		}
		return arrestsAndNotices.thenCompose((ignore) -> {
			return formatter.formatWithPunishment(
					enforcementOptions.replaceTargetArgument(section.successNotification()),
					punishment
			);
		}).thenCompose((notification) -> {
			boolean silent = enforcementOptions.broadcasting() == Broadcasting.SILENT;
			return envEnforcer.sendToThoseWithPermission(
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
		return futureNotify.thenCompose((notification) -> {
			boolean silent = enforcementOptions.broadcasting() == EnforcementOptions.Broadcasting.SILENT;
			return envEnforcer.sendToThoseWithPermission(
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
			logger.trace("Fetching punishment details for id {} and type {}", id, type);
			return selector.getHistoricalPunishmentByIdAndType(id, type).thenCompose((optPunishment) -> {
				if (optPunishment.isEmpty()) {
					// Possible race condition if punishment is expunged
					logger.debug("Tried to unenforce non-existent punishment with id {} and type {}", id, type);
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

	@Override
	public CentralisedFuture<Void> clearExpungedWithoutSynchronization(long id) {
		muteCache.clearCachedMute(id);
		return completedFuture(null);
	}

	@Override
	public CentralisedFuture<Void> updateDetailsWithoutSynchronization(Punishment punishment) {
		return ((SecurePunishment) punishment).enforcePunishment(
				punishment.enforcementOptionsBuilder()
						.broadcasting(Broadcasting.NONE)
						.enforcement(EnforcementOptions.Enforcement.SINGLE_SERVER_ONLY)
						.build()
		);
	}

	@Override
	public CentralisedFuture<Void> updateDetailsWithoutSynchronization(long id) {
		return selector.getHistoricalPunishmentById(id).thenCompose((optPunishment) -> {
			if (optPunishment.isEmpty()) {
				// Possible race condition if punishment is expunged
				logger.debug("Tried to update details of non-existent punishment with id {}", id);
				return completedFuture(null);
			}
			return updateDetailsWithoutSynchronization(optPunishment.get());
		}).toCompletableFuture();
	}

	private CentralisedFuture<Void> enforceArrestsAndNotices(Punishment punishment) {

		return formatter.getPunishmentMessage(punishment).thenCompose((message) -> {

			Victim victim = punishment.getVictim();
			Consumer<P> enforcementCallback = enforcementCallback(punishment, message);
			AddressStrictness strictness = configs.getMainConfig().enforcement().addressStrictness();

			if (victim instanceof PlayerVictim playerVictim) {
				UUID uuid = playerVictim.getUUID();
				if (strictness == AddressStrictness.STRICT) {
					return matchUserPunishmentStrict(uuid, enforcementCallback)
							.thenCompose(envEnforcer::enforceMatcher);
				}
				return envEnforcer.doForPlayerIfOnline(uuid, enforcementCallback);

			} else if (victim instanceof AddressVictim addressVictim) {
				NetworkAddress address = addressVictim.getAddress();
				return matchAddressPunishment(strictness, enforcementCallback, address)
						.thenCompose(envEnforcer::enforceMatcher);

			} else if (victim instanceof CompositeVictim compositeVictim) {
				UUID uuid = compositeVictim.getUUID();
				NetworkAddress address = compositeVictim.getAddress();
				return matchAddressPunishment(strictness, enforcementCallback, address)
						.thenApply((addressMatcher) -> new AdditionalUUIDTargetMatcher<>(uuid, addressMatcher))
						.thenCompose(envEnforcer::enforceMatcher);

			} else {
				throw MiscUtil.unknownVictimType(victim.getType());
			}
		});
	}

	private Consumer<P> enforcementCallback(Punishment punishment, Component message) {
		return switch (punishment.getType()) {
			case BAN, KICK -> (player) -> {
				if (instanceType == InstanceType.GAME_SERVER
						&& configs.getMainConfig().platforms().gameServers().usePluginMessaging()) {
					// Try to kick by plugin message
					if (envEnforcer.sendPluginMessageIfListening(
							player, new KickPlayer(), new KickPlayer.Data(envEnforcer.getNameFor(player), message)
					)) {
						// Kicked by plugin message
						return;
					}
					// This scenario is a remote possibility if the player joins and is very quickly kicked.
					// That is, the plugin messaging channel is activated a small period after the join event.
					// Using the existing APIs on Bukkit and Sponge, there is no solution to this problem without
					// sacrificing the following important warning message for misconfigured setups.
					logger.warn("Attempted to send plugin message to {}, but it could not be sent.", player);
				}
				envEnforcer.kickPlayer(player, message);
			};
			case MUTE -> (player) -> {
				/*
				 * Mute enforcement must additionally take into account the mute cache
				 */
				UUID uuid = envEnforcer.getUniqueIdFor(player);
				NetworkAddress address = NetworkAddress.of(envEnforcer.getAddressFor(player));
				muteCache.setCachedMute(uuid, address, punishment);

				envEnforcer.sendMessageNoPrefix(player, message);
			};
			case WARN -> (player) -> envEnforcer.sendMessageNoPrefix(player, message);
		};
	}

	private CentralisedFuture<TargetMatcher<P>> matchAddressPunishment(
			AddressStrictness strictness, Consumer<P> enforcementCallback, NetworkAddress address) {
		return switch (strictness) {
			case LENIENT -> completedFuture(new ExactTargetMatcher<>(address, enforcementCallback));
			case NORMAL -> matchAddressPunishmentNormal(address, enforcementCallback);
			case STERN, STRICT -> matchAddressPunishmentSternOrStrict(address, enforcementCallback);
		};
	}

	private CentralisedFuture<TargetMatcher<P>> matchAddressPunishmentNormal(
			NetworkAddress address, Consumer<P> enforcementCallback) {
		return queryExecutor.get().query(SQLFunction.readOnly((context) -> {
			return context
					.select(ADDRESSES.UUID)
					.from(ADDRESSES)
					.where(ADDRESSES.ADDRESS.eq(address))
					.fetchSet(ADDRESSES.UUID);
		})).thenApply((uuids) -> {
			return new UUIDTargetMatcher<>(uuids, enforcementCallback);
		});
	}

	private CentralisedFuture<TargetMatcher<P>> matchAddressPunishmentSternOrStrict(
			NetworkAddress address, Consumer<P> enforcementCallback) {
		return queryExecutor.get().query(SQLFunction.readOnly((context) -> {
			return context
					.select(STRICT_LINKS.UUID2)
					.from(STRICT_LINKS)
					.innerJoin(ADDRESSES)
					.on(STRICT_LINKS.UUID1.eq(ADDRESSES.UUID))
					.where(ADDRESSES.ADDRESS.eq(address))
					.fetchSet(STRICT_LINKS.UUID2);
		})).thenApply((uuids) -> {
			return new UUIDTargetMatcher<>(uuids, enforcementCallback);
		});
	}

	private CentralisedFuture<TargetMatcher<P>> matchUserPunishmentStrict(
			UUID uuid, Consumer<P> enforcementCallback) {
		return queryExecutor.get().query(SQLFunction.readOnly((context) -> {
			return context
					.select(STRICT_LINKS.UUID2)
					.from(STRICT_LINKS)
					.where(STRICT_LINKS.UUID1.eq(uuid))
					.fetchSet(STRICT_LINKS.UUID2);
		})).thenApply((uuids) -> {
			return new UUIDTargetMatcher<>(uuids, enforcementCallback);
		});
	}

	private <T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory.completedFuture(value);
	}

}
