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
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.ResultQuery;
import org.jooq.impl.DSL;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.sql.DeserializedVictim;
import space.arim.libertybans.core.database.sql.EndTimeCondition;
import space.arim.libertybans.core.database.sql.SerializedVictim;
import space.arim.libertybans.core.punish.PunishmentCreator;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.ArrayList;
import java.util.List;

import static space.arim.libertybans.core.schema.tables.SimpleActive.SIMPLE_ACTIVE;
import static space.arim.libertybans.core.schema.tables.SimpleHistory.SIMPLE_HISTORY;

@Singleton
public class SelectionImpl {

	private final FactoryOfTheFuture futuresFactory;
	private final Provider<InternalDatabase> dbProvider;
	private final PunishmentCreator creator;
	private final Time time;

	@Inject
	public SelectionImpl(FactoryOfTheFuture futuresFactory, Provider<InternalDatabase> dbProvider,
						 PunishmentCreator creator, Time time) {
		this.futuresFactory = futuresFactory;
		this.dbProvider = dbProvider;
		this.creator = creator;
		this.time = time;
	}

	/*
	 * The objective is to dynamically build SQL queries while avoiding
	 * security-poor concatenated SQL.
	 * 
	 * When selecting non-active punishments from the history table,
	 * the SELECT statement must be qualified by the punishment type
	 */

	private static List<Field<?>> getColumns(InternalSelectionOrder selection) {
		List<Field<?>> columns = new ArrayList<>();
		boolean active = selection.selectActiveOnly();
		columns.add(active ? SIMPLE_ACTIVE.ID : SIMPLE_HISTORY.ID);
		if (selection.getTypeNullable() == null) {
			columns.add(active ? SIMPLE_ACTIVE.TYPE : SIMPLE_HISTORY.TYPE);
		}
		if (selection.getVictimNullable() == null) {
			columns.add(active ? SIMPLE_ACTIVE.VICTIM_TYPE : SIMPLE_HISTORY.VICTIM_TYPE);
			columns.add(active ? SIMPLE_ACTIVE.VICTIM_UUID : SIMPLE_HISTORY.VICTIM_UUID);
			columns.add(active ? SIMPLE_ACTIVE.VICTIM_ADDRESS : SIMPLE_HISTORY.VICTIM_ADDRESS);
		}
		if (selection.getOperatorNullable() == null) {
			columns.add(active ? SIMPLE_ACTIVE.OPERATOR : SIMPLE_HISTORY.OPERATOR);
		}
		columns.add(active ? SIMPLE_ACTIVE.REASON : SIMPLE_HISTORY.REASON);
		if (selection.getScopeNullable() == null) {
			columns.add(active ? SIMPLE_ACTIVE.SCOPE : SIMPLE_HISTORY.SCOPE);
		}
		columns.add(active ? SIMPLE_ACTIVE.START : SIMPLE_HISTORY.START);
		columns.add(active ? SIMPLE_ACTIVE.END : SIMPLE_HISTORY.END);

		return columns;
	}

	private Condition getPredication(InternalSelectionOrder selection) {
		Condition condition = DSL.noCondition();
		boolean active = selection.selectActiveOnly();
		PunishmentType type = selection.getTypeNullable();
		if (type != null) {
			condition = condition
					.and((active ? SIMPLE_ACTIVE.TYPE : SIMPLE_HISTORY.TYPE).eq(type));
		}
		if (active) {
			condition = condition
					.and(new EndTimeCondition(SIMPLE_ACTIVE.END).isNotExpired(time.currentTimestamp()));
		}
		Victim victim = selection.getVictimNullable();
		if (victim != null) {
			SerializedVictim serializedVictim = new SerializedVictim(victim);
			condition = condition
					.and((active ? SIMPLE_ACTIVE.VICTIM_TYPE : SIMPLE_HISTORY.VICTIM_TYPE).eq(victim.getType()))
					.and((active ? SIMPLE_ACTIVE.VICTIM_UUID : SIMPLE_HISTORY.VICTIM_UUID).eq(serializedVictim.uuid()))
					.and((active ? SIMPLE_ACTIVE.VICTIM_ADDRESS : SIMPLE_HISTORY.VICTIM_ADDRESS).eq(serializedVictim.address()));
		}
		Operator operator = selection.getOperatorNullable();
		if (operator != null) {
			condition = condition
					.and((active ? SIMPLE_ACTIVE.OPERATOR : SIMPLE_HISTORY.OPERATOR).eq(operator));
		}
		ServerScope scope = selection.getScopeNullable();
		if (scope != null) {
			condition = condition
					.and((active ? SIMPLE_ACTIVE.SCOPE : SIMPLE_HISTORY.SCOPE).eq(scope));
		}
		return condition;
	}

	private Punishment fromRecordAndSelection(org.jooq.Record record, InternalSelectionOrder selection) {
		boolean active = selection.selectActiveOnly();
		PunishmentType type = selection.getTypeNullable();
		if (type == null) {
			type = record.get(active ? SIMPLE_ACTIVE.TYPE : SIMPLE_HISTORY.TYPE);
		}
		Victim victim = selection.getVictimNullable();
		if (victim == null) {
			Victim.VictimType victimType = record.get(active ? SIMPLE_ACTIVE.VICTIM_TYPE : SIMPLE_HISTORY.VICTIM_TYPE);
			victim = new DeserializedVictim(
					record.get(active ? SIMPLE_ACTIVE.VICTIM_UUID : SIMPLE_HISTORY.VICTIM_UUID),
					record.get(active ? SIMPLE_ACTIVE.VICTIM_ADDRESS : SIMPLE_HISTORY.VICTIM_ADDRESS)
			).victim(victimType);
		}
		Operator operator = selection.getOperatorNullable();
		if (operator == null) {
			operator = record.get(active ? SIMPLE_ACTIVE.OPERATOR : SIMPLE_HISTORY.OPERATOR);
		}
		ServerScope scope = selection.getScopeNullable();
		if (scope == null) {
			scope = record.get(active ? SIMPLE_ACTIVE.SCOPE : SIMPLE_HISTORY.SCOPE);
		}
		return creator.createPunishment(
				record.get(active ? SIMPLE_ACTIVE.ID : SIMPLE_HISTORY.ID),
				type,
				victim,
				operator,
				record.get(active ? SIMPLE_ACTIVE.REASON : SIMPLE_HISTORY.REASON),
				scope,
				record.get(active ? SIMPLE_ACTIVE.START : SIMPLE_HISTORY.START),
				record.get(active ? SIMPLE_ACTIVE.END : SIMPLE_HISTORY.END));
	}

	private ResultQuery<org.jooq.Record> getSelectionQuery(InternalSelectionOrder selection,
														   DSLContext context,
														   boolean singlePunishment) {
		boolean active = selection.selectActiveOnly();
		var query = context
				.select(getColumns(selection))
				.from(active ? SIMPLE_ACTIVE : SIMPLE_HISTORY)
				.where(getPredication(selection))
				.orderBy((active ? SIMPLE_ACTIVE.START : SIMPLE_HISTORY.START).desc())
				.offset(selection.skipCount());
		if (singlePunishment) {
			return query.limit(1);
		}
		int maximumToRetrieve = selection.maximumToRetrieve();
		if (maximumToRetrieve == 0) {
			return query;
		} else {
			return query.limit(maximumToRetrieve - selection.skipCount());
		}
	}

	CentralisedFuture<Punishment> getFirstSpecificPunishment(InternalSelectionOrder selection) {
		if (selection.selectActiveOnly() && selection.getTypeNullable() == PunishmentType.KICK) {
			// Kicks cannot possibly be active. They are all history
			return futuresFactory.completedFuture(null);
		}
		InternalDatabase database = dbProvider.get();
		return database.query(SQLFunction.readOnly((context) -> {
			return getSelectionQuery(selection, context, true)
					.fetchOne((record) -> fromRecordAndSelection(record, selection));
		}));
	}

	ReactionStage<List<Punishment>> getSpecificPunishments(InternalSelectionOrder selection) {
		if (selection.selectActiveOnly() && selection.getTypeNullable() == PunishmentType.KICK) {
			// Kicks cannot possibly be active. They are all history
			return futuresFactory.completedFuture(List.of());
		}
		InternalDatabase database = dbProvider.get();
		return database.query(SQLFunction.readOnly((context) -> {
			return getSelectionQuery(selection, context, false)
					.fetch((record) -> fromRecordAndSelection(record, selection));
		}));
	}

}
