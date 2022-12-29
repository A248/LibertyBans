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
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.Select;
import org.jooq.Table;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.SelectionPredicate;
import space.arim.libertybans.api.select.SortPunishments;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.sql.ApplicableViewFields;
import space.arim.libertybans.core.database.sql.EndTimeCondition;
import space.arim.libertybans.core.database.sql.PunishmentFields;
import space.arim.libertybans.core.database.sql.SimpleViewFields;
import space.arim.libertybans.core.database.sql.TableForType;
import space.arim.libertybans.core.punish.PunishmentCreator;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.jooq.impl.DSL.choose;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.noCondition;
import static org.jooq.impl.DSL.noField;
import static org.jooq.impl.DSL.val;
import static space.arim.libertybans.core.schema.tables.ApplicableActive.APPLICABLE_ACTIVE;
import static space.arim.libertybans.core.schema.tables.ApplicableHistory.APPLICABLE_HISTORY;
import static space.arim.libertybans.core.schema.tables.SimpleActive.SIMPLE_ACTIVE;
import static space.arim.libertybans.core.schema.tables.SimpleHistory.SIMPLE_HISTORY;

public abstract class SelectionBaseSQL extends SelectionBaseImpl {

	private final Resources resources;

	SelectionBaseSQL(Details details, Resources resources) {
		super(details);
		this.resources = resources;
	}

	public record Resources(FactoryOfTheFuture futuresFactory,
							Provider<InternalDatabase> dbProvider,
							PunishmentCreator creator,
							Time time) {
		@Inject
		public Resources {}
	}

	/*
	 * The objective is to dynamically build SQL queries while avoiding
	 * security-poor concatenated SQL.
	 */

	private <F extends PunishmentFields> F determineFields(Function<TableForType, F> forType,
														  Function<Boolean, F> forActiveOrHistorical) {
		boolean active = selectActiveOnly();
		if (active && getTypes().isSimpleEquality()) {
			PunishmentType type = getTypes().acceptedValues().iterator().next();
			return forType.apply(new TableForType(type));
		} else {
			return forActiveOrHistorical.apply(active);
		}
	}

	SimpleViewFields<?> requestSimpleView() {
		return determineFields(
				TableForType::simpleView,
				(active) -> active ? new SimpleViewFields<>(SIMPLE_ACTIVE) : new SimpleViewFields<>(SIMPLE_HISTORY)
		);
	}

	ApplicableViewFields<?> requestApplicableView() {
		return determineFields(
				TableForType::applicableView,
				(active) -> active ?
						new ApplicableViewFields<>(APPLICABLE_ACTIVE) : new ApplicableViewFields<>(APPLICABLE_HISTORY)
		);
	}

	static <U> Condition matchesCriterion(SelectionPredicate<U> selection, Field<U> field) {
		Set<U> acceptedValues = selection.acceptedValues();
		Set<U> rejectedValues = selection.rejectedValues();

		Condition acceptedCondition = switch (acceptedValues.size()) {
			case 0 -> noCondition();
			case 1 -> field.eq(inlineIfNeeded(field, acceptedValues.iterator().next()));
			default -> field.in(acceptedValues);
		};
		Condition notRejectedCondition = switch (rejectedValues.size()) {
			case 0 -> noCondition();
			case 1 -> field.notEqual(inlineIfNeeded(field, rejectedValues.iterator().next()));
			default -> field.notIn(rejectedValues);
		};
		return acceptedCondition.and(notRejectedCondition);
	}

	private static <U> Field<U> inlineIfNeeded(Field<U> field, U value) {
		// Automatically inline PunishmentType and VictimType comparisons
		Class<U> fieldType = field.getType();
		boolean shouldInline = fieldType.equals(PunishmentType.class) || fieldType.equals(Victim.VictimType.class);
		return shouldInline ? inline(value) : val(value);
	}

	static <U> U retrieveValueFromRecordOrSelection(SelectionPredicate<U> selection, Record record,
													Field<U> field) {
		if (selection.isSimpleEquality()) {
			return selection.acceptedValues().iterator().next();
		}
		return record.get(field);
	}

	abstract class QueryBuilder {

		final PunishmentFields fields;
		private final List<Field<?>> additionalColumns;
		private final Condition additionalPredication;

		QueryBuilder(PunishmentFields fields, List<Field<?>> additionalColumns,
					 Condition additionalPredication) {
			this.fields = fields;
			this.additionalColumns = additionalColumns;
			this.additionalPredication = additionalPredication;
		}

		abstract Victim victimFromRecord(Record record);

		private List<Field<?>> getColumnsToRetrieve() {
			List<Field<?>> columns = new ArrayList<>();
			columns.add(fields.id());
			columns.addAll(additionalColumns);
			if (getTypes().isNotSimpleEquality()) {
				columns.add(fields.type());
			}
			if (getOperators().isNotSimpleEquality()) {
				columns.add(fields.operator());
			}
			columns.add(fields.reason());
			if (getScopes().isNotSimpleEquality()) {
				columns.add(fields.scope());
			}
			columns.add(fields.start());
			columns.add(fields.end());
			return columns;
		}

		private Condition getPredication(Supplier<Instant> timeSupplier) {
			Condition condition = noCondition();
			boolean active = selectActiveOnly();

			if (active && getTypes().isSimpleEquality()) {
				// Type is ensured by selected table
			} else {
				condition = condition
						.and(matchesCriterion(getTypes(), fields.type()));
			}
			condition = condition
					.and(matchesCriterion(getOperators(), fields.operator()))
					.and(matchesCriterion(getScopes(), fields.scope()))
					.and(additionalPredication);
			if (active) {
				condition = condition
						.and(new EndTimeCondition(fields).isNotExpired(timeSupplier.get()));
			}
			Instant seekAfterStartTime = seekAfterStartTime();
			Instant seekBeforeStartTime = seekBeforeStartTime();
			if (!seekAfterStartTime.equals(Instant.EPOCH)) {
				long seekAfterId = seekAfterId();
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
			if (!seekBeforeStartTime.equals(Instant.MAX)) {
				long seekBeforeId = seekBeforeId();
				if (seekBeforeId == Long.MAX_VALUE) {
					// start <= seekBeforeStartTime
					condition = condition.and(fields.start().lessOrEqual(seekBeforeStartTime));
				} else {
					// start < seekBeforeStartTime OR (start = seekBeforeStartTime AND id <= seekBeforeId)
					condition = condition.and(
							fields.start().lessThan(seekBeforeStartTime).or(
									fields.start().eq(seekBeforeStartTime).and(fields.id().lessOrEqual(seekBeforeId))
							)
					);
				}
			}
			return condition;
		}

		private Punishment mapRecord(Record record) {
			PunishmentType type = retrieveValueFromRecordOrSelection(
					getTypes(), record, fields.type()
			);
			Victim victim = victimFromRecord(
					record
			);
			Operator operator = retrieveValueFromRecordOrSelection(
					getOperators(), record, fields.operator()
			);
			ServerScope scope = retrieveValueFromRecordOrSelection(
					getScopes(), record, fields.scope()
			);
			return resources.creator.createPunishment(
					record.get(fields.id()),
					type,
					victim,
					operator,
					record.get(fields.reason()),
					scope,
					record.get(fields.start()),
					record.get(fields.end())
			);
		}

		/*
		In some databases, fields referenced in ORDER BY must be included in the SELECT clause
		 */
		private List<OrderField<?>> buildOrdering(SortPunishments[] ordering) {

			List<OrderField<?>> orderFields = new ArrayList<>(ordering.length * 2);
			for (SortPunishments sortPunishments : ordering) {
				switch (sortPunishments) {
				case NEWEST_FIRST -> {
					orderFields.add(fields.start().desc());
					orderFields.add(fields.id().desc());
				}
				case OLDEST_FIRST -> {
					orderFields.add(fields.start().asc());
					orderFields.add(fields.id().asc());
				}
				case LATEST_END_DATE_FIRST -> {
					var end = fields.end().coerce(Long.class);
					// Since, end = 0 defines a permanent punishment use
					// CASE WHEN end = 0 THEN Long.MAX_VALUE ELSE end END
					orderFields.add(choose(end)
							.when(inline(0L), inline(Long.MAX_VALUE))
							.otherwise(end)
							.desc());
				}
				case SOONEST_END_DATE_FIRST -> {
					var end = fields.end().coerce(Long.class);
					orderFields.add(choose(end)
							.when(inline(0L), inline(Long.MAX_VALUE))
							.otherwise(end)
							.asc());
				}
				}
			}
			return orderFields;
		}

		Query<?> constructSelect(QueryParameters parameters, Table<?> table) {
			List<Field<?>> selection = getColumnsToRetrieve();
			List<OrderField<?>> ordering = buildOrdering(parameters.ordering);
			return new Query<>(
					parameters.context
							.select(selection)
							.from(table)
							.where(getPredication(parameters.timeSupplier))
							.orderBy(ordering)
							.offset((skipCount() == 0) ? noField(Integer.class) : val(skipCount()))
							.limit(parameters.limit),
					this::mapRecord
			);
		}

	}

	record Query<R extends Record>(Select<R> select, RecordMapper<R, Punishment> mapper) {

		Punishment fetchOne() {
			return select.fetchOne(mapper);
		}

		List<Punishment> fetch() {
			return select.fetch(mapper);
		}

		String renderSQL() {
			return select.getSQL();
		}
	}

	record QueryParameters(DSLContext context, Field<Integer> limit,
						   Supplier<Instant> timeSupplier, SortPunishments...ordering) {}

	abstract Query<?> requestQuery(QueryParameters parameters);

	private boolean selectActiveKicks() {
		return selectActiveOnly()
				&& getTypes().isSimpleEquality()
				&& getTypes().acceptedValues().iterator().next() == PunishmentType.KICK;
	}

	/**
	 * Visible for testing purposes, to make sure queries are as optimized as possible
	 *
	 * @param context the database access
	 * @return the SQL which would be used to select a single applicable punishment
	 */
	public String renderSingleApplicablePunishmentSQL(DSLContext context) {
		return requestQuery(
				new QueryParameters(context, inline(1), () -> Instant.EPOCH, SortPunishments.LATEST_END_DATE_FIRST)
		).renderSQL();
	}

	/**
	 * Visible for internal use, for the efficiency of reusing a database connection
	 * during execution of incoming logins
	 *
	 * @param context the database access
	 * @param timeSupplier the current time supplier
	 * @param prioritization sorting prioritization
	 * @return finds a single punishment from this selection
	 */
	public Punishment findFirstSpecificPunishment(DSLContext context, Supplier<Instant> timeSupplier,
												  SortPunishments...prioritization) {
		return requestQuery(
				new QueryParameters(context, inline(1), timeSupplier, prioritization)
		).fetchOne();
	}

	@Override
	public ReactionStage<Optional<Punishment>> getFirstSpecificPunishment(SortPunishments...prioritization) {
		if (selectActiveKicks()) {
			// Kicks cannot possibly be active. They are all history
			return resources.futuresFactory.completedFuture(Optional.empty());
		}
		return resources.dbProvider.get()
				.query(SQLFunction.readOnly((context) -> {
					return findFirstSpecificPunishment(context, resources.time::currentTimestamp, prioritization);
				}))
				.thenApply(Optional::ofNullable);
	}

	@Override
	public ReactionStage<List<Punishment>> getAllSpecificPunishments(SortPunishments...ordering) {
		if (selectActiveKicks()) {
			// Kicks cannot possibly be active
			return resources.futuresFactory.completedFuture(List.of());
		}
		return resources.dbProvider.get().query(SQLFunction.readOnly((context) -> requestQuery(
				new QueryParameters(
						context,
						(limitToRetrieve() == 0) ? noField(Integer.class) : val(limitToRetrieve()),
						resources.time::currentTimestamp,
						ordering
				)
		).fetch()));
	}

	@Override
	public ReactionStage<Integer> countNumberOfPunishments() {
		if (selectActiveKicks()) {
			// Kicks cannot possibly be active
			return resources.futuresFactory.completedFuture(0);
		}
		return resources.dbProvider.get().query(SQLFunction.readOnly((context) -> {
			Query<?> query = requestQuery(
					new QueryParameters(
							context,
							(limitToRetrieve() == 0) ? noField(Integer.class) : val(limitToRetrieve()),
							resources.time::currentTimestamp
					)
			);
			return context
					.select(count())
					.from(query.select)
					.fetchSingle()
					.value1();
		}));
	}

}
