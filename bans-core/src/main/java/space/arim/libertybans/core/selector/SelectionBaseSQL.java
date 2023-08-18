/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.SQLDialect;
import org.jooq.Select;
import org.jooq.Table;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.SelectionPredicate;
import space.arim.libertybans.api.select.SortPunishments;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.sql.ApplicableViewFields;
import space.arim.libertybans.core.database.sql.EndTimeCondition;
import space.arim.libertybans.core.database.sql.PunishmentFields;
import space.arim.libertybans.core.database.sql.ScopeCondition;
import space.arim.libertybans.core.database.sql.SimpleViewFields;
import space.arim.libertybans.core.database.sql.TableForType;
import space.arim.libertybans.core.scope.ScopeType;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.jooq.impl.DSL.*;
import static space.arim.libertybans.core.schema.tables.ApplicableActive.APPLICABLE_ACTIVE;
import static space.arim.libertybans.core.schema.tables.ApplicableHistory.APPLICABLE_HISTORY;
import static space.arim.libertybans.core.schema.tables.SimpleActive.SIMPLE_ACTIVE;
import static space.arim.libertybans.core.schema.tables.SimpleHistory.SIMPLE_HISTORY;

public abstract class SelectionBaseSQL extends SelectionBaseImpl {

	private final SelectionResources resources;

	SelectionBaseSQL(Details details, SelectionResources resources) {
		super(details);
		this.resources = resources;
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

	abstract class QueryBuilder {

		private final QueryParameters parameters;
		final PunishmentFields fields;
		private final Table<?> table;

		QueryBuilder(QueryParameters parameters, PunishmentFields fields, Table<?> table) {
			this.parameters = parameters;
			this.fields = fields;
			this.table = table;
		}

		abstract Victim victimFromRecord(Record record);

		abstract boolean mightRepeatIds();

		private boolean groupByIdForDeduplication() {
			return mightRepeatIds() && parameters.limit != 1;
		}

		private <U> Field<U> aggregate(Field<U> field) {
			if (parameters.context.family() == SQLDialect.POSTGRES) {
				Class<U> fieldType = field.getType();
				// PostgreSQL does not define MAX/MIN for the UUID or BYTEA types
				if (fieldType.equals(UUID.class) || fieldType.equals(NetworkAddress.class) || fieldType.equals(Operator.class)) {
					return arrayGet(arrayAgg(
							field(field.getQualifiedName(), fieldType)
					), inline(1));
				}
			}
			return max(field);
		}

		private List<Field<?>> getColumnsToRetrieve(List<Field<?>> additionalColumns) {
			List<Field<?>> columns = new ArrayList<>(additionalColumns);
			if (getTypes().isNotSimpleEquality()) {
				columns.add(fields.type());
			}
			if (getOperators().isNotSimpleEquality()) {
				columns.add(fields.operator());
			}
			columns.add(fields.reason());
			if (getScopes().isNotSimpleEquality()) {
				columns.add(fields.scopeType());
				columns.add(fields.scope());
			}
			columns.add(fields.start());
			columns.add(fields.end());
			if (getEscalationTracks().isNotSimpleEquality()) {
				columns.add(fields.track());
			}
			if (groupByIdForDeduplication()) {
				columns.replaceAll(this::aggregate);
			}
			columns.add(fields.id());
			return columns;
		}

		private Condition getPredication(Supplier<Instant> timeSupplier) {
			Condition condition = noCondition();
			boolean active = selectActiveOnly();

			if (active && getTypes().isSimpleEquality()) {
				// Type is ensured by selected table
			} else {
				condition = condition
						.and(new SingleFieldCriterion<>(fields.type()).matches(getTypes()));
			}
			condition = condition
					.and(new SingleFieldCriterion<>(fields.operator()).matches(getOperators()))
					.and(new SingleFieldCriterion<>(fields.track()).matches(getEscalationTracks(), (optTrack) -> optTrack.orElse(null)))
					.and(new ScopeCondition(fields, resources.scopeManager()).buildCondition(getScopes()));
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

		<U> Field<U> aggregateIfNeeded(Field<U> field) {
			return groupByIdForDeduplication() ? aggregate(field) : field;
		}

		private <F, G> F retrieveValueFromRecordOrSelection(
				SelectionPredicate<G> selection, Record record, Field<F> field, Function<G, F> converter) {
			if (selection.isSimpleEquality()) {
				return converter.apply(selection.acceptedValues().iterator().next());
			}
			return record.get(aggregateIfNeeded(field));
		}

		<F> F retrieveValueFromRecordOrSelection(SelectionPredicate<F> selection, Record record, Field<F> field) {
			return retrieveValueFromRecordOrSelection(selection, record, field, Function.identity());
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
			ServerScope scope;
			if (getScopes().isSimpleEquality()) {
				scope = getScopes().acceptedValues().iterator().next();
			} else {
				ScopeType scopeType = record.get(aggregateIfNeeded(fields.scopeType()));
				String scopeValue = record.get(aggregateIfNeeded(fields.scope()));
				scope = resources.scopeManager().deserialize(scopeType, scopeValue);
			}
			EscalationTrack escalationTrack = retrieveValueFromRecordOrSelection(
					getEscalationTracks(), record, fields.track(),
					(optTrack) -> optTrack.orElse(null)
			);
			return resources.creator().createPunishment(
					record.get(fields.id()),
					type,
					victim,
					operator,
					record.get(aggregateIfNeeded(fields.reason())),
					scope,
					record.get(aggregateIfNeeded(fields.start())),
					record.get(aggregateIfNeeded(fields.end())),
					escalationTrack
			);
		}

		private List<OrderField<?>> buildOrdering(SortPunishments[] ordering) {
			Field<Instant> start = aggregateIfNeeded(fields.start());
			Field<Long> end = aggregateIfNeeded(fields.end()).coerce(Long.class);
			// Since end = 0 defines a permanent punishment, use
			// CASE WHEN end = 0 THEN Long.MAX_VALUE ELSE end END
			end = choose(end)
					.when(inline(0L), inline(Long.MAX_VALUE))
					.otherwise(end);

			List<OrderField<?>> orderFields = new ArrayList<>(ordering.length * 2);
			for (SortPunishments sortPunishments : ordering) {
				switch (sortPunishments) {
				case NEWEST_FIRST -> {
					orderFields.add(start.desc());
					orderFields.add(fields.id().desc());
				}
				case OLDEST_FIRST -> {
					orderFields.add(start.asc());
					orderFields.add(fields.id().asc());
				}
				case LATEST_END_DATE_FIRST -> orderFields.add(end.desc());
				case SOONEST_END_DATE_FIRST -> orderFields.add(end.asc());
				}
			}
			return orderFields;
		}

		Query<?> constructSelect(List<Field<?>> additionalColumns, Condition additionalPredication) {
			return new Query<>(
					parameters.context
							.select(getColumnsToRetrieve(additionalColumns))
							.from(table)
							.where(getPredication(parameters.timeSupplier).and(additionalPredication))
							.groupBy(groupByIdForDeduplication() ? fields.id() : noField())
							.orderBy(buildOrdering(parameters.ordering))
							.offset((skipCount() == 0) ? noField(Integer.class) : val(skipCount()))
							.limit(switch (parameters.limit) {
								case 0 -> noField(Integer.class);
								case 1 -> inline(1);
								default -> val(parameters.limit);
							}),
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

	record QueryParameters(DSLContext context, int limit,
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
				new QueryParameters(context, 1, () -> Instant.EPOCH, SortPunishments.LATEST_END_DATE_FIRST)
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
				new QueryParameters(context, 1, timeSupplier, prioritization)
		).fetchOne();
	}

	@Override
	public ReactionStage<Optional<Punishment>> getFirstSpecificPunishment(SortPunishments...prioritization) {
		if (selectActiveKicks()) {
			// Kicks cannot possibly be active. They are all history
			return resources.futuresFactory().completedFuture(Optional.empty());
		}
		return resources.dbProvider().get()
				.query(SQLFunction.readOnly((context) -> {
					return findFirstSpecificPunishment(context, resources.time()::currentTimestamp, prioritization);
				}))
				.thenApply(Optional::ofNullable);
	}

	@Override
	public ReactionStage<List<Punishment>> getAllSpecificPunishments(SortPunishments...ordering) {
		if (selectActiveKicks()) {
			// Kicks cannot possibly be active
			return resources.futuresFactory().completedFuture(List.of());
		}
		return resources.dbProvider().get().query(SQLFunction.readOnly((context) -> requestQuery(
				new QueryParameters(
						context,
						limitToRetrieve(),
						resources.time()::currentTimestamp,
						ordering
				)
		).fetch()));
	}

	@Override
	public ReactionStage<Integer> countNumberOfPunishments() {
		if (selectActiveKicks()) {
			// Kicks cannot possibly be active
			return resources.futuresFactory().completedFuture(0);
		}
		return resources.dbProvider().get().query(SQLFunction.readOnly((context) -> {
			Query<?> query = requestQuery(
					new QueryParameters(
							context,
							limitToRetrieve(),
							resources.time()::currentTimestamp
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
