/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

package space.arim.libertybans.core.selector;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.DSLContext;
import space.arim.api.env.annote.PlatformPlayer;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.SelectionPredicate;
import space.arim.libertybans.api.select.SortPunishments;
import space.arim.libertybans.core.alts.*;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.pagination.InstantThenUUID;
import space.arim.libertybans.core.database.pagination.KeysetPage;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.punish.Association;
import space.arim.libertybans.core.service.FuturePoster;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Gatekeeper {

	private final Configs configs;
	private final FuturePoster futurePoster;
	private final FactoryOfTheFuture futuresFactory;
	private final Provider<QueryExecutor> queryExecutor;
	private final InternalFormatter formatter;
	private final ConnectionLimiter connectionLimiter;
	private final AltDetection altDetection;
	private final AltNotification altNotification;
	private final Time time;

	@Inject
	public Gatekeeper(Configs configs, FuturePoster futurePoster, FactoryOfTheFuture futuresFactory,
					  Provider<QueryExecutor> queryExecutor, InternalFormatter formatter,
					  ConnectionLimiter connectionLimiter, AltDetection altDetection, AltNotification altNotification,
					  Time time) {
		this.configs = configs;
        this.futurePoster = futurePoster;
        this.futuresFactory = futuresFactory;
		this.queryExecutor = queryExecutor;
		this.formatter = formatter;
		this.connectionLimiter = connectionLimiter;
		this.altDetection = altDetection;
		this.altNotification = altNotification;
		this.time = time;
	}

	CentralisedFuture<Component> executeAndCheckConnection(UUID uuid, String name, NetworkAddress address,
														   Set<ServerScope> scopes, SelectorImpl selector) {
		return queryExecutor.get().queryWithRetry((context, transaction) -> {
			Instant currentTime = time.currentTimestamp();
			EnforcementConfig config = configs.getMainConfig().enforcement();

			boolean recordUserAssociation = config.altsRegistry().shouldRegisterOnConnection();
			if (recordUserAssociation) {
				doAssociation(uuid, name, address, currentTime, context);
			}

			Punishment ban = selector.selectionByApplicabilityBuilder(uuid, address)
					.type(PunishmentType.BAN)
					.scopes(SelectionPredicate.matchingAnyOf(scopes))
					.canAssumeUserRecorded(recordUserAssociation)
					.build()
					.findFirstSpecificPunishment(context, currentTime, SortPunishments.LATEST_END_DATE_FIRST);
			if (ban != null) {
				return ban;
			}
			Component connectionLimitMessage = connectionLimiter.hasExceededLimit(context, address, currentTime);
			if (connectionLimitMessage != null) {
				return connectionLimitMessage;
			}
			// The player may join, but should be checked for alts
			EnforcementConfig.AltsAutoShow altsAutoShow = configs.getMainConfig().enforcement().altsAutoShow();
			if (altsAutoShow.enable() && !altsAutoShow.enableBypassPermission()) {
				var formatting = configs.getMessagesConfig().alts().autoShow();
                return altDetection.detectAlts(context, new AltInfoRequest(
						uuid, address, altsAutoShow.showWhichAlts(), formatting.oldestFirst(), formatting.limit()
				));
			}
			return null;
		}).thenCompose((banOrLimitMessageOrDetectedAltsOrNull) -> {
			if (banOrLimitMessageOrDetectedAltsOrNull instanceof Punishment punishment) {
				return formatter.getPunishmentMessage(punishment);
			}
			if (banOrLimitMessageOrDetectedAltsOrNull instanceof Component limitMsg) {
				return futuresFactory.completedFuture(limitMsg);
			}
			if (banOrLimitMessageOrDetectedAltsOrNull instanceof KeysetPage<?, ?> altResponse) {
				// Offload the alt notification so as not to block the incoming login
				@SuppressWarnings("unchecked")
                KeysetPage<DetectedAlt, InstantThenUUID> detectedAlts = (KeysetPage<DetectedAlt, InstantThenUUID>) altResponse;
				futurePoster.postFuture(altNotification.notifyFoundAlts(detectedAlts, name));
			}
			return futuresFactory.completedFuture(null);
		});
	}

	public CentralisedFuture<@Nullable Component> checkServerSwitch(UUID uuid, String name, NetworkAddress address,
																	String destinationServer, ServerScope serverScope,
																	SelectorImpl selector) {
		if (!configs.getMainConfig().platforms().proxies().enforceServerSwitch()) {
			return futuresFactory.completedFuture(null);
		}
		return queryExecutor.get().queryWithRetry((context, transaction) -> {
			Instant currentTime = time.currentTimestamp();
			var altsRegistry = configs.getMainConfig().enforcement().altsRegistry();
			boolean registerOnConnection = altsRegistry.shouldRegisterOnConnection();
			List<String> serversWithoutAssociation = altsRegistry.serversWithoutRegistration();

			boolean recordUserAssociation = !registerOnConnection && !serversWithoutAssociation.contains(destinationServer);
			if (recordUserAssociation) {
				doAssociation(uuid, name, address, currentTime, context);
			}

			return selector.selectionByApplicabilityBuilder(uuid, address)
					.type(PunishmentType.BAN)
					.scopes(SelectionPredicate.matchingOnly(serverScope))
					.canAssumeUserRecorded(registerOnConnection || recordUserAssociation)
					.build()
					.findFirstSpecificPunishment(context, currentTime, SortPunishments.LATEST_END_DATE_FIRST);
		}).thenCompose((punishment) -> {
			if (punishment != null) {
				return formatter.getPunishmentMessage(punishment);
			} else {
				return futuresFactory.completedFuture(null);
			}
		});
	}

	private void doAssociation(UUID uuid, String name, NetworkAddress address,
							   Instant currentTime, DSLContext context) {
		Association association = new Association(uuid, context);
		association.associateCurrentName(name, currentTime);
		association.associateCurrentAddress(address, currentTime);
	}

	<@PlatformPlayer P> CentralisedFuture<Void> onJoin(P player, EnvEnforcer<P> envEnforcer) {
		EnforcementConfig.AltsAutoShow altsAutoShow = configs.getMainConfig().enforcement().altsAutoShow();
		if (altsAutoShow.enable() && altsAutoShow.enableBypassPermission()) {
			boolean bypass = envEnforcer.hasPermission(player, "libertybans.alts.bypass.autoshow");
			if (!bypass) {
				return notifyAlts(player, envEnforcer, altsAutoShow.showWhichAlts());
			}
		}
		return futuresFactory.completedFuture(null);
	}

	private <P> CentralisedFuture<Void> notifyAlts(P player, EnvEnforcer<P> envEnforcer, WhichAlts whichAlts) {
		UUID uuid = envEnforcer.getUniqueIdFor(player);
		NetworkAddress address = NetworkAddress.of(envEnforcer.getAddressFor(player));
		String name = envEnforcer.getNameFor(player);

		var formatting = configs.getMessagesConfig().alts().autoShow();
		AltInfoRequest request = new AltInfoRequest(
				uuid, address, whichAlts, formatting.oldestFirst(), formatting.limit()
		);
		return queryExecutor.get().query(SQLFunction.readOnly(
				(context) -> altDetection.detectAlts(context, request)
		)).thenCompose(
				(detectedAlts) -> altNotification.notifyFoundAlts(detectedAlts, name)
		);
	}
}
