/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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
import org.jooq.Field;
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
			if (selection.getVictimTypes().isNotSimpleEquality()) {
				columns.add(fields.victimType());
			}
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

		Condition acceptedCondition = switch (acceptedValues.size()) {
			case 0 -> DSL.noCondition();
			case 1 -> field.eq(acceptedValues.iterator().next());
			default -> field.in(acceptedValues);
		};
		Condition notRejectedCondition = switch (rejectedValues.size()) {
			case 0 -> DSL.noCondition();
			case 1 -> field.notEqual(rejectedValues.iterator().next());
			default -> field.notIn(rejectedValues);
		};
		return acceptedCondition.and(notRejectedCondition);
	}

	private Condition getPredication(SelectionOrder selection, PunishmentFields fields) {
		Condition condition = DSL.noCondition();
		boolean active = selection.selectActiveOnly();

		condition = condition
				.and(matchesCriterion(selection.getTypes(), fields.type()))
				.and(matchesCriterion(selection.getVictimTypes(), fields.victimType()))
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
		Instant seekAfterStartTime = selection.seekAfterStartTime();
		Instant seekBeforeStartTime = selection.seekBeforeStartTime();
		if (!seekAfterStartTime.equals(Instant.EPOCH)) {
			long seekAfterId = selection.seekAfterId();
			if (seekAfterId == 0L) {
				// Optimization since seekAfterId is irrelevant
				// start >= seekAfterStartTime
				condition = condition.and(fields.start().greaterOrEqual(seekAfterStartTime));
			} else {
				// start > seekAfterStartTime OR (start = seekAfterStartTime AND id >= seekAfterId)
				condition = condition.and(
						fields.start().greaterThan(seekAfterStartTime).or(
								fields.start().eq(seekAfterStartTime).and(fields.id().greaterOrEqual(seekAfterId))
						)
				);
			}
		}
		if (!seekBeforeStartTime.equals(Instant.EPOCH)) {
			long seekBeforeId = selection.seekBeforeId();
			if (seekBeforeId == Long.MAX_VALUE) {
				condition = condition.and(fields.start().lessOrEqual(seekBeforeStartTime));
			} else {
				condition = condition.and(
						fields.start().lessThan(seekBeforeStartTime).or(
								fields.start().eq(seekBeforeStartTime).and(fields.id().lessOrEqual(seekBeforeId))
						)
				);
			}
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
			Victim.VictimType victimType = retrieveValueFromRecordOrSelection(
					selection.getVictimTypes(), record, fields.victimType()
			);
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
			return context
					.select(getColumns(selection, fields))
					.from(fields.table())
					.where(getPredication(selection, fields))
					.orderBy(
							fields.start().desc(), fields.id().desc()
					)
					.offset(selection.skipCount())
					.limit(1)
					.fetchOne((record) -> fromRecordAndSelection(record, selection, fields));
		}));
	}

	ReactionStage<List<Punishment>> getSpecificPunishments(SelectionOrder selection) {
		if (selectActiveKicks(selection)) {
			// Kicks cannot possibly be active. They are all history
			return futuresFactory.completedFuture(List.of());
		}
		return dbProvider.get().query(SQLFunction.readOnly((context) -> {
			PunishmentFields fields = getPunishmentFieldsToUse(selection);
			return context
					.select(getColumns(selection, fields))
					.from(fields.table())
					.where(getPredication(selection, fields))
					.orderBy(
							fields.start().desc(), fields.id().desc()
					)
					.offset(selection.skipCount())
					.limit((selection.limitToRetrieve() == 0) ?
							DSL.noField(Integer.class) : DSL.val(selection.limitToRetrieve())
					)
					.fetch((record) -> fromRecordAndSelection(record, selection, fields));
		}));
	}

	ReactionStage<Integer> countNumberOfPunishments(SelectionOrder selection) {
		if (selectActiveKicks(selection)) {
			// Kicks cannot possibly be active.
			return futuresFactory.completedFuture(0);
		}
		return dbProvider.get().query(SQLFunction.readOnly((context) -> {
			PunishmentFields fields = getPunishmentFieldsToUse(selection);
			return context
					.select(DSL.count())
					.from(fields.table())
					.where(getPredication(selection, fields))
					.orderBy(
							fields.start().desc(), fields.id().desc()
					)
					.offset(selection.skipCount())
					.limit((selection.limitToRetrieve() == 0) ?
							DSL.noField(Integer.class) : DSL.val(selection.limitToRetrieve())
					)
					.fetchSingle()
					.value1();
		}));
	}

	private PunishmentFields getPunishmentFieldsToUse(SelectionOrder selection) {
		if (selection.selectActiveOnly()) {
			if (selection.getTypes().isSimpleEquality()) {
				PunishmentType type = selection.getTypes().acceptedValues().iterator().next();
				return new TableForType(type).simpleView();
			} else {
				return new SimpleViewFields<>(SimpleActive.SIMPLE_ACTIVE);
			}
		} else {
			return new SimpleViewFields<>(SimpleHistory.SIMPLE_HISTORY);
		}
	}
}
