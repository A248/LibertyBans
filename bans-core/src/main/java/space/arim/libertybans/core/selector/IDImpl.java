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

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.core.punish.PunishmentCreator;

@Singleton
public class IDImpl {

	private final FactoryOfTheFuture futuresFactory;
	private final Provider<InternalDatabase> dbProvider;
	private final PunishmentCreator creator;
	private final Time time;
	
	@Inject
	public IDImpl(FactoryOfTheFuture futuresFactory, Provider<InternalDatabase> dbProvider,
				  PunishmentCreator creator, Time time) {
		this.futuresFactory = futuresFactory;
		this.dbProvider = dbProvider;
		this.creator = creator;
		this.time = time;
	}
	
	CentralisedFuture<Punishment> getActivePunishmentById(int id) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			return database.jdbCaesar().transaction().body((querySource, controller) -> {

				long currentTime = time.currentTime();
				for (PunishmentType type : MiscUtil.punishmentTypesExcludingKick()) {

					Punishment found = querySource.query(
							"SELECT `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end` "
							+ "FROM `libertybans_simple_" + type + "s` "
							+ "WHERE `id` = ? AND (`end` = 0 OR `end` > ?)")
							.params(id, currentTime)
							.singleResult((resultSet) -> {
								return creator.createPunishment(id, type,
										database.getVictimFromResult(resultSet), database.getOperatorFromResult(resultSet),
										database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
										database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
							}).execute();
					if (found != null) {
						return found;
					}
				}
				return null;
			}).execute();
		});
	}
	
	CentralisedFuture<Punishment> getActivePunishmentByIdAndType(int id, PunishmentType type) {
		if (type == PunishmentType.KICK) {
			// Kicks are never active
			return futuresFactory.completedFuture(null);
		}
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			return database.jdbCaesar().query(
					"SELECT `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end` "
					+ "FROM `libertybans_simple_" + type + "s` "
					+ "WHERE `id` = ? AND (`end` = 0 OR `end` > ?)")
					.params(id, time.currentTime())
					.singleResult((resultSet) -> {
						return creator.createPunishment(id, type,
								database.getVictimFromResult(resultSet), database.getOperatorFromResult(resultSet),
								database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
								database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
					}).execute();
		});
	}
	
	CentralisedFuture<Punishment> getHistoricalPunishmentById(int id) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			return database.jdbCaesar().query(
					"SELECT `type`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end` "
					+ "FROM `libertybans_simple_history` WHERE `id` = ?")
					.params(id)
					.singleResult((resultSet) -> {
						return creator.createPunishment(id, database.getTypeFromResult(resultSet),
								database.getVictimFromResult(resultSet), database.getOperatorFromResult(resultSet),
								database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
								database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
					}).execute();
		});
	}

}
