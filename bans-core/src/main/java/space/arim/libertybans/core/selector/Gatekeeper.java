/*
 * LibertyBans
 * Copyright © 2024 Anand Beh
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
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.SelectionPredicate;
import space.arim.libertybans.api.select.SortPunishments;
import space.arim.libertybans.core.alts.AltDetection;
import space.arim.libertybans.core.alts.AltNotification;
import space.arim.libertybans.core.alts.ConnectionLimiter;
import space.arim.libertybans.core.alts.DetectedAlt;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.punish.Association;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Gatekeeper {

	private final Configs configs;
	private final FactoryOfTheFuture futuresFactory;
	private final Provider<QueryExecutor> queryExecutor;
	private final InternalFormatter formatter;
	private final ConnectionLimiter connectionLimiter;
	private final AltDetection altDetection;
	private final AltNotification altNotification;
	private final Time time;

	@Inject
	public Gatekeeper(Configs configs, FactoryOfTheFuture futuresFactory, Provider<QueryExecutor> queryExecutor,
					  InternalFormatter formatter, ConnectionLimiter connectionLimiter, AltDetection altDetection,
					  AltNotification altNotification, Time time) {
		this.configs = configs;
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
					.findFirstSpecificPunishment(context, () -> currentTime, SortPunishments.LATEST_END_DATE_FIRST);
			if (ban != null) {
				return ban;
			}
			Component connectionLimitMessage = connectionLimiter.hasExceededLimit(context, address, currentTime);
			if (connectionLimitMessage != null) {
				return connectionLimitMessage;
			}
			// The player may join, but should be checked for alts
			EnforcementConfig.AltsAutoShow altsAutoShow = configs.getMainConfig().enforcement().altsAutoShow();
			if (altsAutoShow.enable()) {
				List<DetectedAlt> detectedAlts = altDetection.detectAlts(context, uuid, address, altsAutoShow.showWhichAlts());
				return detectedAlts;
			}
			return null;
		}).thenCompose((banOrLimitMessageOrDetectedAltsOrNull) -> {
			if (banOrLimitMessageOrDetectedAltsOrNull instanceof Punishment) {
				return formatter.getPunishmentMessage((Punishment) banOrLimitMessageOrDetectedAltsOrNull);
			}
			if (banOrLimitMessageOrDetectedAltsOrNull instanceof Component) {
				return futuresFactory.completedFuture((Component) banOrLimitMessageOrDetectedAltsOrNull);
			}
			if (banOrLimitMessageOrDetectedAltsOrNull instanceof List) {
				@SuppressWarnings("unchecked")
				List<DetectedAlt> detectedAlts = (List<DetectedAlt>) banOrLimitMessageOrDetectedAltsOrNull;
				altNotification.notifyFoundAlts(uuid, name, address, detectedAlts);
			}
			return futuresFactory.completedFuture(null);
		});
	}

	public CentralisedFuture<@Nullable Component> checkServerSwitch(UUID uuid, String name, NetworkAddress address,
																	String destinationServer, ServerScope scope, SelectorImpl selector) {
		if (!configs.getMainConfig().platforms().proxies().enforceServerSwitch()) {
			return null;
		}

		return queryExecutor.get().queryWithRetry((context, transaction) -> {
			var altsRegistry = configs.getMainConfig().enforcement().altsRegistry();
			boolean registerOnConnection = altsRegistry.shouldRegisterOnConnection();
			List<String> serversWithoutAssociation = altsRegistry.serversWithoutRegistration();

			if (!registerOnConnection && !serversWithoutAssociation.contains(destinationServer)) {
				doAssociation(uuid, name, address, time.currentTimestamp(), context);
			}

			return selector.selectionByApplicabilityBuilder(uuid, address)
					.type(PunishmentType.BAN)
					.scope(scope)
					.build()
					.getFirstSpecificPunishment(SortPunishments.LATEST_END_DATE_FIRST);

		}).thenCompose((punishment) -> {
			if (punishment instanceof Punishment) {
				return formatter.getPunishmentMessage((Punishment) punishment);
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
}
