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
import space.arim.libertybans.api.select.SelectionOrder;
import space.arim.libertybans.api.select.SelectionPredicate;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.sql.DeserializedVictim;
import space.arim.libertybans.core.database.sql.EndTimeCondition;
import space.arim.libertybans.core.database.sql.SimpleViewFields;
import space.arim.libertybans.core.database.sql.VictimCondition;
import space.arim.libertybans.core.punish.PunishmentCreator;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

	private static List<Field<?>> getColumns(SelectionOrder selection) {
		List<Field<?>> columns = new ArrayList<>();
		boolean active = selection.selectActiveOnly();
		columns.add(active ? SIMPLE_ACTIVE.ID : SIMPLE_HISTORY.ID);
		if (selection.getTypes().isNotSimpleEquality()) {
			columns.add(active ? SIMPLE_ACTIVE.TYPE : SIMPLE_HISTORY.TYPE);
		}
		if (selection.getVictims().isNotSimpleEquality()) {
			columns.add(active ? SIMPLE_ACTIVE.VICTIM_TYPE : SIMPLE_HISTORY.VICTIM_TYPE);
			columns.add(active ? SIMPLE_ACTIVE.VICTIM_UUID : SIMPLE_HISTORY.VICTIM_UUID);
			columns.add(active ? SIMPLE_ACTIVE.VICTIM_ADDRESS : SIMPLE_HISTORY.VICTIM_ADDRESS);
		}
		if (selection.getOperators().isNotSimpleEquality()) {
			columns.add(active ? SIMPLE_ACTIVE.OPERATOR : SIMPLE_HISTORY.OPERATOR);
		}
		columns.add(active ? SIMPLE_ACTIVE.REASON : SIMPLE_HISTORY.REASON);
		if (selection.getScopes().isNotSimpleEquality()) {
			columns.add(active ? SIMPLE_ACTIVE.SCOPE : SIMPLE_HISTORY.SCOPE);
		}
		columns.add(active ? SIMPLE_ACTIVE.START : SIMPLE_HISTORY.START);
		columns.add(active ? SIMPLE_ACTIVE.END : SIMPLE_HISTORY.END);

		return columns;
	}

	private static <U> Condition matchesCriterion(SelectionPredicate<U> selection, Field<U> field) {
		Set<U> acceptedValues = selection.acceptedValues();
		Set<U> rejectedValues = selection.rejectedValues();
		if (acceptedValues.isEmpty() && rejectedValues.isEmpty()) {
			// Accepts everything
			return DSL.noCondition();
		}
		Condition acceptedCondition = (acceptedValues.size() == 1) ?
				field.eq(acceptedValues.iterator().next()) : field.in(acceptedValues);
		Condition notRejectedCondition = (rejectedValues.size() == 1) ?
				field.notEqual(rejectedValues.iterator().next()) : field.notIn(rejectedValues);
		return acceptedCondition.and(notRejectedCondition);
	}

	private Condition getPredication(SelectionOrder selection) {
		Condition condition = DSL.noCondition();
		boolean active = selection.selectActiveOnly();

		condition = condition
				.and(matchesCriterion(selection.getTypes(), active ? SIMPLE_ACTIVE.TYPE : SIMPLE_HISTORY.TYPE))
				.and(matchesCriterion(selection.getOperators(), active ? SIMPLE_ACTIVE.OPERATOR : SIMPLE_HISTORY.OPERATOR))
				.and(matchesCriterion(selection.getScopes(), active ? SIMPLE_ACTIVE.SCOPE : SIMPLE_HISTORY.SCOPE));
		if (active) {
			condition = condition
					.and(new EndTimeCondition(SIMPLE_ACTIVE.END).isNotExpired(time.currentTimestamp()));
		}
		SelectionPredicate<Victim> victims = selection.getVictims();
		{
			VictimCondition victimCondition = new VictimCondition(
					new SimpleViewFields(active ? SIMPLE_ACTIVE : SIMPLE_HISTORY)
			);
			Condition acceptedCondition = DSL.noCondition();
			for (Victim acceptedVictim : victims.acceptedValues()) {
				acceptedCondition = acceptedCondition.or(
						victimCondition.matchesVictim(acceptedVictim)
				);
			}
			Condition notRejectedCondition = DSL.noCondition();
			for (Victim rejectedVictim : victims.rejectedValues()) {
				Condition notEqual = DSL.not(
						victimCondition.matchesVictim(rejectedVictim)
				);
				notRejectedCondition = notRejectedCondition.and(notEqual);
			}
			condition = condition.and(acceptedCondition).and(notRejectedCondition);
		}
		return condition;
	}

	private static <U> U retrieveValueFromRecordOrSelection(SelectionPredicate<U> selection, org.jooq.Record record,
															Field<U> field) {
		if (selection.isSimpleEquality()) {
			return selection.acceptedValues().iterator().next();
		}
		return record.get(field);
	}

	private Punishment fromRecordAndSelection(org.jooq.Record record, SelectionOrder selection) {
		boolean active = selection.selectActiveOnly();

		PunishmentType type = retrieveValueFromRecordOrSelection(
				selection.getTypes(), record, active ? SIMPLE_ACTIVE.TYPE : SIMPLE_HISTORY.TYPE
		);
		Victim victim;
		SelectionPredicate<Victim> victimSelection = selection.getVictims();
		if (victimSelection.isSimpleEquality()) {
			victim = victimSelection.acceptedValues().iterator().next();
		} else {
			Victim.VictimType victimType = record.get(active ? SIMPLE_ACTIVE.VICTIM_TYPE : SIMPLE_HISTORY.VICTIM_TYPE);
			victim = new DeserializedVictim(
					record.get(active ? SIMPLE_ACTIVE.VICTIM_UUID : SIMPLE_HISTORY.VICTIM_UUID),
					record.get(active ? SIMPLE_ACTIVE.VICTIM_ADDRESS : SIMPLE_HISTORY.VICTIM_ADDRESS)
			).victim(victimType);
		}
		Operator operator = retrieveValueFromRecordOrSelection(
				selection.getOperators(), record, active ? SIMPLE_ACTIVE.OPERATOR : SIMPLE_HISTORY.OPERATOR
		);
		ServerScope scope = retrieveValueFromRecordOrSelection(
				selection.getScopes(), record, active ? SIMPLE_ACTIVE.SCOPE : SIMPLE_HISTORY.SCOPE
		);
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

	private ResultQuery<org.jooq.Record> selectMatchingPunishments(SelectionOrder selection,
																   DSLContext context,
																   boolean singlePunishment) {
		boolean active = selection.selectActiveOnly();
		var selectOrderBy = context
				.select(getColumns(selection))
				.from(active ? SIMPLE_ACTIVE : SIMPLE_HISTORY)
				.where(getPredication(selection))
				.orderBy(
						(active ? SIMPLE_ACTIVE.START : SIMPLE_HISTORY.START).desc(),
						(active ? SIMPLE_ACTIVE.ID : SIMPLE_HISTORY.ID).desc()
				);
		// If limit == 0, omit LIMIT clause entirely
		int limit = (singlePunishment) ? 1 : selection.limitToRetrieve();

		Instant seekAfterStartTime = selection.seekAfterStartTime();
		int offset = selection.skipCount();
		if (offset == 0) {
			if (seekAfterStartTime.equals(Instant.EPOCH)) {
				// No OFFSET and no SEEK AFTER
				return (limit == 0) ? selectOrderBy : selectOrderBy.limit(limit);
			} else {
				// Has SEEK AFTER
				var seekAfterQuery = selectOrderBy.seekAfter(seekAfterStartTime, selection.seekAfterId());
				return (limit == 0) ? seekAfterQuery : seekAfterQuery.limit(limit);
			}
		} else {
			// Has OFFSET
			assert seekAfterStartTime.equals(Instant.EPOCH) : "seekAfter is exclusive with skipFirstRetrieved";
			var offsetQuery = selectOrderBy.offset(offset);
			return (limit == 0) ? offsetQuery : offsetQuery.limit(limit);
		}
	}

	private static boolean selectActiveKicks(SelectionOrder selection) {
		return selection.selectActiveOnly()
				&& selection.getTypes().isSimpleEquality()
				&& selection.getTypes().acceptedValues().iterator().next() == PunishmentType.KICK;
	}

	CentralisedFuture<Punishment> getFirstSpecificPunishment(SelectionOrder selection) {
		if (selectActiveKicks(selection)) {
			// Kicks cannot possibly be active. They are all history
			return futuresFactory.completedFuture(null);
		}
		InternalDatabase database = dbProvider.get();
		return database.query(SQLFunction.readOnly((context) -> {
			return selectMatchingPunishments(selection, context, true)
					.fetchOne((record) -> fromRecordAndSelection(record, selection));
		}));
	}

	ReactionStage<List<Punishment>> getSpecificPunishments(SelectionOrder selection) {
		if (selectActiveKicks(selection)) {
			// Kicks cannot possibly be active. They are all history
			return futuresFactory.completedFuture(List.of());
		}
		InternalDatabase database = dbProvider.get();
		return database.query(SQLFunction.readOnly((context) -> {
			return selectMatchingPunishments(selection, context, false)
					.fetch((record) -> fromRecordAndSelection(record, selection));
		}));
	}

}
