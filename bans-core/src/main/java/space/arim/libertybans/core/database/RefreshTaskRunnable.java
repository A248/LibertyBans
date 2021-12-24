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

package space.arim.libertybans.core.database;

import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.config.SqlConfig;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.core.service.Time;

import java.time.Duration;
import java.time.Instant;

import static space.arim.libertybans.core.schema.tables.Messages.MESSAGES;

/**
 * Responsible for periodically purging expired punishments and expired messages
 *
 */
class RefreshTaskRunnable implements Runnable {

	private final DatabaseManager manager;
	private final InternalDatabase database;
	private final Time time;

	private static final Duration MESSAGE_EXPIRATION_TIME = Duration.ofMinutes(1L);

	RefreshTaskRunnable(DatabaseManager manager, InternalDatabase database, Time time) {
		this.manager = manager;
		this.database = database;
		this.time = time;
	}

	static Duration obtainPollRate(SqlConfig.Synchronization synchronizationConf) {
		Duration pollRate = Duration.ofMillis(synchronizationConf.pollRateMillis());
		// Make the max poll rate less than the expiration time to allow for some leniency
		// Reasons for this include clock desynchronization and server lag
		Duration maxPollRate = MESSAGE_EXPIRATION_TIME.minus(Duration.ofSeconds(30L));
		assert !maxPollRate.isZero() && !maxPollRate.isNegative() : "Negative or zero: " + maxPollRate;

		if (pollRate.compareTo(maxPollRate) >= 0) {
			throw new IllegalStateException(
					"poll-rate-millis setting must be less than " + maxPollRate);
		}
		return pollRate;
	}

	@Override
	public void run() {
		if (manager.getInternal() != database) {
			// cancelled but not stopped yet, or failed to stop
			LoggerFactory.getLogger(getClass()).warn("Refresh task continues after shutdown");
			return;
		}
		// These DELETE queries may delete many rows. As such, they are run in single-query transactions
		// Grouping them together in the same transaction would require unnecessary exertion from the RDMS
		for (PunishmentType type : MiscUtil.punishmentTypesExcludingKick()) {
			database.executeWithRetry(((context, transaction) -> {
				Instant currentTime = time.currentTimestamp();
				database.clearExpiredPunishments(context, type, currentTime);
			})).join();
		}
		if (manager.configs().getSqlConfig().synchronization().enabled()) {
			database.executeWithRetry((context, transaction) -> {
				Instant deleteMessagesBefore = time.currentTimestamp().minus(MESSAGE_EXPIRATION_TIME);
				context
						.deleteFrom(MESSAGES)
						.where(MESSAGES.TIME.lessOrEqual(deleteMessagesBefore))
						.execute();
			}).join();
		}
	}
}
