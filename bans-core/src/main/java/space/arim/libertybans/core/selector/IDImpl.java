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

package space.arim.libertybans.core.selector;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.sql.EndTimeCondition;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.punish.PunishmentCreator;

import static space.arim.libertybans.core.schema.tables.SimpleActive.SIMPLE_ACTIVE;
import static space.arim.libertybans.core.schema.tables.SimpleHistory.SIMPLE_HISTORY;

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

	CentralisedFuture<Punishment> getActivePunishmentById(long id) {
		InternalDatabase database = dbProvider.get();
		return database.query(SQLFunction.readOnly((context) -> {
			return context
					.selectFrom(SIMPLE_ACTIVE)
					.where(SIMPLE_ACTIVE.ID.eq(id))
					.and(new EndTimeCondition(SIMPLE_ACTIVE.END).isNotExpired(time.currentTimestamp()))
					.fetchOne(creator.punishmentMapper());
		}));
	}

	CentralisedFuture<Punishment> getActivePunishmentByIdAndType(long id, PunishmentType type) {
		if (type == PunishmentType.KICK) {
			// Kicks are never active
			return futuresFactory.completedFuture(null);
		}
		InternalDatabase database = dbProvider.get();
		return database.query(SQLFunction.readOnly((context) -> {
			return context
					.select(
							SIMPLE_ACTIVE.VICTIM_TYPE, SIMPLE_ACTIVE.VICTIM_UUID, SIMPLE_ACTIVE.VICTIM_ADDRESS,
							SIMPLE_ACTIVE.OPERATOR, SIMPLE_ACTIVE.REASON,
							SIMPLE_ACTIVE.SCOPE, SIMPLE_ACTIVE.START, SIMPLE_ACTIVE.END
					)
					.from(SIMPLE_ACTIVE)
					.where(SIMPLE_ACTIVE.ID.eq(id))
					.and(SIMPLE_ACTIVE.TYPE.eq(type))
					.and(new EndTimeCondition(SIMPLE_ACTIVE.END).isNotExpired(time.currentTimestamp()))
					.fetchOne(creator.punishmentMapper(id, type));
		}));
	}

	CentralisedFuture<Punishment> getHistoricalPunishmentById(long id) {
		InternalDatabase database = dbProvider.get();
		return database.query(SQLFunction.readOnly((context) -> {
			return context
					.selectFrom(SIMPLE_HISTORY)
					.where(SIMPLE_HISTORY.ID.eq(id))
					.fetchOne(creator.punishmentMapper());
		}));
	}

}
