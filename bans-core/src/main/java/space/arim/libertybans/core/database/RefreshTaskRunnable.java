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
package space.arim.libertybans.core.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.MiscUtil;

class RefreshTaskRunnable implements Runnable {

	private final DatabaseManager manager;
	private final Database database;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	RefreshTaskRunnable(DatabaseManager manager, Database database) {
		this.manager = manager;
		this.database = database;
	}
	
	@Override
	public void run() {
		if (manager.getCurrentDatabase() != database) {
			// cancelled but not stopped yet, or failed to stop
			logger.debug("HSQLDB cleaning task continues after shutdown");
			return;
		}
		long currentTime = MiscUtil.currentTime();
		database.jdbCaesar().transaction().transactor((querySource) -> {

			for (PunishmentType type : MiscUtil.punishmentTypesExcludingKick()) {
				querySource.query(
						"DELETE FROM `libertybans_" + type.getLowercaseNamePlural() + "` WHERE `id` IN "
						+ "(SELECT `id` FROM `libertybans_punishments` `puns` WHERE (`puns`.`end` != 0 AND `puns`.`end` < ?))")
						.params(currentTime)
						.voidResult().execute();
			}
			return (Void) null;
		}).onRollback(() -> null).execute();
	}
}
