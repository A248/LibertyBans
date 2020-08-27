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

import java.util.Set;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.MiscUtil;
import space.arim.libertybans.core.SecurePunishment;
import space.arim.libertybans.core.database.Database;

class OtherImpl extends SelectorImplGroup {

	OtherImpl(Selector selector) {
		super(selector);
	}
	
	CentralisedFuture<Set<Punishment>> getHistoryForVictim(Victim victim) {
		Database database = core().getDatabase();
		return database.selectAsync(() -> {

			Set<Punishment> result = database.jdbCaesar().query(
						"SELECT `id`, `type`, `operator`, `reason`, `scope`, `start`, `end`, `undone` FROM "
						+ "`libertybans_history` WHERE `victim` = ? AND `victim_type` = ?")
					.params(victim, victim.getType())
					.setResult((resultSet) -> {
						return (Punishment) new SecurePunishment(resultSet.getInt("id"),
								database.getTypeFromResult(resultSet), victim, database.getOperatorFromResult(resultSet),
								database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
								database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
					}).onError(Set::of).execute();
			return Set.copyOf(result);
		});
	}

	CentralisedFuture<Set<Punishment>> getActivePunishmentsForType(PunishmentType type) {
		if (type == PunishmentType.KICK) {
			// Kicks are never active
			return core().getFuturesFactory().completedFuture(Set.of());
		}
		Database database = core().getDatabase();
		return database.selectAsync(() -> {

			String table = "`libertybans_" + type.getLowercaseNamePlural() + '`';
			Set<Punishment> result = database.jdbCaesar().query(
					"SELECT `id`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end` FROM "
					+ table + " WHERE `end` = 0 OR `end` > ?")
					.params(MiscUtil.currentTime())
					.setResult((resultSet) -> {
						return (Punishment) new SecurePunishment(resultSet.getInt("id"), type,
								database.getVictimFromResult(resultSet), database.getOperatorFromResult(resultSet),
								database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
								database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
					}).onError(Set::of).execute();
			return Set.copyOf(result);
		});
	}

}
