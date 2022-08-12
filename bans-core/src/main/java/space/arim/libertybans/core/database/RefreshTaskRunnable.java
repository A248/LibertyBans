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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.ThisClass;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

import static space.arim.libertybans.core.schema.tables.Messages.MESSAGES;

/**
 * Responsible for periodically purging expired punishments and expired messages
 *
 */
public final class RefreshTaskRunnable implements Runnable {

	private final DatabaseManager manager;
	private final InternalDatabase database;
	private final Time time;

	/*
	We do not want punishments to expire before their messages are polled.
	So, we make the max poll rate 30 seconds less than the expiration time;
	the extra latency accounts for clock desynchronization and server lag.
	 */
	public static final long MAX_POLL_RATE_MILLIS = 30 * 1000L;
	private static final Duration MESSAGE_EXPIRATION_TIME = Duration.ofMillis(MAX_POLL_RATE_MILLIS).plusSeconds(30L);

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	RefreshTaskRunnable(DatabaseManager manager, InternalDatabase database, Time time) {
		this.manager = manager;
		this.database = database;
		this.time = time;
	}

	@Override
	public void run() {
		if (manager.getInternal() != database) {
			// cancelled but not stopped yet, or failed to stop
			logger.warn("Refresh task continues after shutdown");
			return;
		}
		try (Connection connection = database.getConnection()) {
			// These DELETE queries may delete many rows. As such, they are run in single-query transactions
			// Grouping them together in the same transaction would require unnecessary exertion from the RDMS
			Instant currentTime = time.currentTimestamp();
			for (PunishmentType type : MiscUtil.punishmentTypesExcludingKick()) {
				database.executeWithExistingConnection(connection, (context, transaction) -> {
					database.clearExpiredPunishments(context, type, currentTime);
				});
			}
			if (manager.configs().getSqlConfig().synchronization().enabled()) {
				Instant deleteMessagesBefore = currentTime.minus(MESSAGE_EXPIRATION_TIME);
				database.executeWithExistingConnection(connection, (context, transaction) -> {
					context
							.deleteFrom(MESSAGES)
							.where(MESSAGES.TIME.lessOrEqual(deleteMessagesBefore))
							.execute();
				});
			}
		} catch (SQLException ex) {
			// Note that we have no retry logic. This could be due to serialization failure.
			// However, it is reasonable to expect the RDMS to retry single-query transactions
			logger.warn("Failed to clear expired punishments or messages", ex);
		}
	}
}
