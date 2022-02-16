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
import space.arim.libertybans.core.database.sql.PunishmentFields;
import space.arim.libertybans.core.database.sql.SimpleViewFields;
import space.arim.libertybans.core.database.sql.TableForType;
import space.arim.libertybans.core.database.sql.VictimCondition;
import space.arim.libertybans.core.punish.PunishmentCreator;
import space.arim.libertybans.core.schema.tables.SimpleActive;
import space.arim.libertybans.core.schema.tables.SimpleHistory;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

	private static List<Field<?>> getColumns(SelectionOrder selection, PunishmentFields fields) {
		List<Field<?>> columns = new ArrayList<>();
		columns.add(fields.id());
		if (selection.getTypes().isNotSimpleEquality()) {
			columns.add(fields.type());
		}
		if (selection.getVictims().isNotSimpleEquality()) {
			columns.add(fields.victimType());
			columns.add(fields.victimUuid());
			columns.add(fields.victimAddress());
		}
		if (selection.getOperators().isNotSimpleEquality()) {
			columns.add(fields.operator());
		}
		columns.add(fields.reason());
		if (selection.getScopes().isNotSimpleEquality()) {
			columns.add(fields.scope());
		}
		columns.add(fields.start());
		columns.add(fields.end());

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

	private Condition getPredication(SelectionOrder selection, PunishmentFields fields) {
		Condition condition = DSL.noCondition();
		boolean active = selection.selectActiveOnly();

		condition = condition
				.and(matchesCriterion(selection.getTypes(), fields.type()))
				.and(matchesCriterion(selection.getOperators(), fields.operator()))
				.and(matchesCriterion(selection.getScopes(), fields.scope()));
		if (active) {
			condition = condition
					.and(new EndTimeCondition(fields).isNotExpired(time.currentTimestamp()));
		}
		SelectionPredicate<Victim> victims = selection.getVictims();
		{
			VictimCondition victimCondition = new VictimCondition(fields);
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

	private Punishment fromRecordAndSelection(org.jooq.Record record,
											  SelectionOrder selection, PunishmentFields fields) {

		PunishmentType type = retrieveValueFromRecordOrSelection(
				selection.getTypes(), record, fields.type()
		);
		Victim victim;
		SelectionPredicate<Victim> victimSelection = selection.getVictims();
		if (victimSelection.isSimpleEquality()) {
			victim = victimSelection.acceptedValues().iterator().next();
		} else {
			Victim.VictimType victimType = record.get(fields.victimType());
			victim = new DeserializedVictim(
					record.get(fields.victimUuid()),
					record.get(fields.victimAddress())
			).victim(victimType);
		}
		Operator operator = retrieveValueFromRecordOrSelection(
				selection.getOperators(), record, fields.operator()
		);
		ServerScope scope = retrieveValueFromRecordOrSelection(
				selection.getScopes(), record, fields.scope()
		);
		return creator.createPunishment(
				record.get(fields.id()),
				type,
				victim,
				operator,
				record.get(fields.reason()),
				scope,
				record.get(fields.start()),
				record.get(fields.end()));
	}

	private ResultQuery<org.jooq.Record> selectMatchingPunishments(SelectionOrder selection,
																   PunishmentFields fields,
																   DSLContext context,
																   boolean singlePunishment) {
		var selectOrderBy = context
				.select(getColumns(selection, fields))
				.from(fields.table())
				.where(getPredication(selection, fields))
				.orderBy(
						fields.start().desc(), fields.id().desc()
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
			PunishmentFields fields = getPunishmentFieldsToUse(selection);
			return selectMatchingPunishments(selection, fields, context, true)
					.fetchOne((record) -> fromRecordAndSelection(record, selection, fields));
		}));
	}

	ReactionStage<List<Punishment>> getSpecificPunishments(SelectionOrder selection) {
		if (selectActiveKicks(selection)) {
			// Kicks cannot possibly be active. They are all history
			return futuresFactory.completedFuture(List.of());
		}
		InternalDatabase database = dbProvider.get();
		return database.query(SQLFunction.readOnly((context) -> {
			PunishmentFields fields = getPunishmentFieldsToUse(selection);
			return selectMatchingPunishments(selection, fields, context, false)
					.fetch((record) -> fromRecordAndSelection(record, selection, fields));
		}));
	}

	private PunishmentFields getPunishmentFieldsToUse(SelectionOrder selection) {
		if (selection.selectActiveOnly()) {
			if (selection.getTypes().isSimpleEquality()) {
				return new TableForType(selection.getTypes().acceptedValues().iterator().next()).simpleView();
			} else {
				return new SimpleViewFields<>(SimpleActive.SIMPLE_ACTIVE);
			}
		} else {
			return new SimpleViewFields<>(SimpleHistory.SIMPLE_HISTORY);
		}
	}
}
