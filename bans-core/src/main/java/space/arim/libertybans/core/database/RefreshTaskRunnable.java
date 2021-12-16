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
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.core.service.Time;

import java.time.Instant;

class RefreshTaskRunnable implements Runnable {

	private final DatabaseManager manager;
	private final InternalDatabase database;
	private final Time time;

	RefreshTaskRunnable(DatabaseManager manager, InternalDatabase database, Time time) {
		this.manager = manager;
		this.database = database;
		this.time = time;
	}

	@Override
	public void run() {
		if (manager.getInternal() != database) {
			// cancelled but not stopped yet, or failed to stop
			LoggerFactory.getLogger(getClass()).warn("Cleaning task continues after shutdown");
			return;
		}
		database.executeWithRetry((context, transaction) -> {

			Instant currentTime = time.currentTimestamp();
			for (PunishmentType type : MiscUtil.punishmentTypesExcludingKick()) {
				database.clearExpiredPunishments(context, type, currentTime);
			}
		}).join();
	}
}
