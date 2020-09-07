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
package space.arim.libertybans.core.selector;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.MiscUtil;
import space.arim.libertybans.core.SecurePunishment;
import space.arim.libertybans.core.database.Database;

class IDImpl extends SelectorImplGroup {

	IDImpl(Selector selector) {
		super(selector);
	}
	
	CentralisedFuture<Punishment> getActivePunishmentById(int id) {
		Database database = core().getDatabase();
		return database.selectAsync(() -> {
			return database.jdbCaesar().transaction().body((querySource, controller) -> {

				long currentTime = MiscUtil.currentTime();
				for (PunishmentType type : MiscUtil.punishmentTypesExcludingKick()) {

					Punishment found = querySource.query(
							"SELECT `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end` "
							+ "FROM `libertybans_simple_" + type.getLowercaseNamePlural() + "` "
							+ "WHERE `id` = ? AND (`end` = 0 OR `end` > ?)")
							.params(id, currentTime)
							.singleResult((resultSet) -> {
								return new SecurePunishment(id, type,
										database.getVictimFromResult(resultSet), database.getOperatorFromResult(resultSet),
										database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
										database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
							}).execute();
					if (found != null) {
						return found;
					}
				}
				return null;
			}).onError(() -> null).execute();
		});
	}
	
	CentralisedFuture<Punishment> getActivePunishmentByIdAndType(int id, PunishmentType type) {
		if (type == PunishmentType.KICK) {
			// Kicks are never active
			return core().getFuturesFactory().completedFuture(null);
		}
		Database database = core().getDatabase();
		return database.selectAsync(() -> {
			return database.jdbCaesar().query(
					"SELECT `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end` "
					+ "FROM `libertybans_simple_" + type.getLowercaseNamePlural() + "` "
					+ "WHERE `id` = ? AND (`end` = 0 OR `end` > ?)")
					.params(id, MiscUtil.currentTime())
					.singleResult((resultSet) -> {
						return new SecurePunishment(id, type,
								database.getVictimFromResult(resultSet), database.getOperatorFromResult(resultSet),
								database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
								database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
					}).onError(() -> null).execute();
		});
	}
	
	CentralisedFuture<Punishment> getHistoricalPunishmentById(int id) {
		Database database = core().getDatabase();
		return database.selectAsync(() -> {
			return database.jdbCaesar().query(
					"SELECT `type`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end` "
					+ "FROM `libertybans_simple_history` WHERE `id` = ?")
					.params(id)
					.singleResult((resultSet) -> {
						return new SecurePunishment(id, database.getTypeFromResult(resultSet),
								database.getVictimFromResult(resultSet), database.getOperatorFromResult(resultSet),
								database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
								database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
					}).onError(() -> null).execute();
		});
	}

}
