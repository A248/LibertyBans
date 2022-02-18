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

package space.arim.libertybans.core.selector;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import net.kyori.adventure.text.Component;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
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

	private final ApplicableImpl applicableImpl;

	@Inject
	public Gatekeeper(Configs configs, FactoryOfTheFuture futuresFactory, Provider<QueryExecutor> queryExecutor,
					  InternalFormatter formatter, ConnectionLimiter connectionLimiter, AltDetection altDetection,
					  AltNotification altNotification, Time time, ApplicableImpl applicableImpl) {
		this.configs = configs;
		this.futuresFactory = futuresFactory;
		this.queryExecutor = queryExecutor;
		this.formatter = formatter;
		this.connectionLimiter = connectionLimiter;
		this.altDetection = altDetection;
		this.altNotification = altNotification;
		this.time = time;
		this.applicableImpl = applicableImpl;
	}

	CentralisedFuture<Component> executeAndCheckConnection(UUID uuid, String name, NetworkAddress address) {
		return queryExecutor.get().queryWithRetry((context, transaction) -> {
			Instant currentTime = time.currentTimestamp();

			Association association = new Association(uuid, context);
			association.associateCurrentName(name, currentTime);
			association.associateCurrentAddress(address, currentTime);

			Punishment ban = applicableImpl.selectApplicable(context, uuid, address, PunishmentType.BAN, currentTime);
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
}
