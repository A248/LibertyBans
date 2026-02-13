/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

import org.jooq.Binding;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
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
import space.arim.libertybans.core.database.pagination.KeysetAnchor;
import space.arim.libertybans.core.database.pagination.Pagination;
import space.arim.libertybans.core.database.pagination.StartTimeThenId;
import space.arim.libertybans.core.database.sql.*;
import space.arim.libertybans.core.scope.ScopeType;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;

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
														   F allActive, F allHistorical) {
		boolean active = selectActiveOnly();
		if (active && getTypes().isSimpleEquality()) {
			PunishmentType type = getTypes().acceptedValues().iterator().next();
			return forType.apply(new TableForType(type));
		} else if (active) {
			return allActive;
		}
		return allHistorical;
	}

	SimpleViewFields requestSimpleView() {
		return determineFields(
				TableForType::simpleView,
				new SimpleActiveFields(SIMPLE_ACTIVE), new SimpleHistoryFields(SIMPLE_HISTORY)
		);
	}

	ApplicableViewFields requestApplicableView() {
		return determineFields(
				TableForType::applicableView,
				new ApplicableActiveFields(APPLICABLE_ACTIVE), new ApplicableHistoryFields(APPLICABLE_HISTORY)
		);
	}

	record Fundamental(Condition victimPredication) {}

	abstract class QueryBuilder {

		private final QueryParameters parameters;
		private final PunishmentFields usingFields;
		private final Table<?> usingTable;

		QueryBuilder(QueryParameters parameters, PunishmentFields usingFields, Table<?> usingTable) {
			this.parameters = parameters;
			this.usingFields = usingFields;
			this.usingTable = usingTable;
		}

		abstract Victim victimFromRecord(Record record, PunishmentFields fields);

		abstract boolean mightRepeatIds();

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

		private final class WithFields {

			private final PunishmentFields fields;

            private WithFields(PunishmentFields fields) {
                this.fields = fields;
            }

			// Columns used excluding victim-related or ID
			private void getRegularColumns(List<Field<?>> output) {
				if (getTypes().isNotSimpleEquality()) {
					output.add(fields.type());
				}
				if (getOperators().isNotSimpleEquality()) {
					output.add(fields.operator());
				}
				output.add(fields.reason());
				if (getScopes().isNotSimpleEquality()) {
					output.add(fields.scopeType());
					output.add(fields.scope());
				}
				output.add(fields.start());
				output.add(fields.end());
				if (getEscalationTracks().isNotSimpleEquality()) {
					output.add(fields.track());
				}
			}

			private Condition regularPredication() {
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
							.and(new EndTimeCondition(fields).isNotExpired(parameters.currentTime));
				}
				Instant seekAfterStartTime = seekAfterStartTime();
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
				Instant seekBeforeStartTime = seekBeforeStartTime();
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
						record, fields
				);
				Operator operator = retrieveValueFromRecordOrSelection(
						getOperators(), record, fields.operator()
				);
				ServerScope scope;
				if (getScopes().isSimpleEquality()) {
					scope = getScopes().acceptedValues().iterator().next();
				} else {
					ScopeType scopeType = record.get(fields.scopeType());
					String scopeValue = record.get(fields.scope());
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
						record.get(fields.reason()),
						scope,
						record.get(fields.start()),
						record.get(fields.end()),
						escalationTrack
				);
			}

			private List<OrderField<?>> traditionalOrdering(SortPunishments[] ordering) {
				Field<Instant> start = fields.start();
				Field<Long> end = fields.end().coerce(Long.class);
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

			private Ordering buildOrdering() {
				KeysetAnchor<StartTimeThenId> pageAnchor = pageAnchor();
				if (pageAnchor == null) {
					return new Ordering(noCondition(), traditionalOrdering(parameters.ordering));
				} else {
					Pagination<StartTimeThenId> pagination = new Pagination<>(
							pageAnchor, false, StartTimeThenId.defineOrder(
							fields.start(), fields.id()
					));
					return new Ordering(pagination.seeking(), Arrays.asList(pagination.order()));
				}
			}
		}

		private <F, G> F retrieveValueFromRecordOrSelection(
				SelectionPredicate<G> selection, Record record, Field<F> field, Function<G, F> converter) {
			if (selection.isSimpleEquality()) {
				return converter.apply(selection.acceptedValues().iterator().next());
			}
			return record.get(field);
		}

		<F> F retrieveValueFromRecordOrSelection(SelectionPredicate<F> selection, Record record, Field<F> field) {
			return retrieveValueFromRecordOrSelection(selection, record, field, Function.identity());
		}

		private record Ordering(Condition predicate, List<OrderField<?>> byFields) {}

		Query<?> constructSelect(List<Field<?>> victimColumns, Condition victimPredication) {
			var dequalifyField = new MappedPunishmentFields.Mapper() {
				@Override
				public <T> Field<T> map(Field<T> original) {
					return renameField(original, original.getQualifiedName());
				}
			};
			WithFields qualified = new WithFields(usingFields);
			WithFields simplified = new WithFields(new MappedPunishmentFields(usingFields, dequalifyField));
			Select<Record> selectQuery;

			Field<Long> fieldId = simplified.fields.id();
			Field<Integer> skipField = (skipCount() == 0) ? noField(Integer.class) : val(skipCount());
			Field<Integer> limitField = switch (parameters.limit) {
				case 0 -> noField(Integer.class);
				case 1 -> inline(1);
				default -> val(parameters.limit);
			};
			if (mightRepeatIds() && parameters.limit != 1) {
				// Perform a subquery where we group by ID to remove duplicates

				List<Field<?>> innerColumns = new ArrayList<>(victimColumns);
				qualified.getRegularColumns(innerColumns);
				innerColumns.replaceAll((field) -> {
					Field<?> aggregated = aggregate(field);
					return aggregated.as("inner_" + field.getName());
				});
				innerColumns.add(fieldId);

				List<Field<?>> outerColumns = new ArrayList<>(innerColumns.size());
				outerColumns.addAll(victimColumns);
				qualified.getRegularColumns(outerColumns);
				outerColumns.replaceAll((field) -> {
					Name innerName = name("inner_" + field.getName());
					return renameField(field, innerName).as(field.getName());
				});
				outerColumns.add(field(fieldId.getUnqualifiedName()));

				var innerQuery = parameters.context
						.select(innerColumns)
						.from(usingTable)
						.where(victimPredication)
						.and(qualified.regularPredication())
						.groupBy(fieldId);

				// Order using the "inner_"-prefixed names where possible
				var innerFieldName =  new MappedPunishmentFields.Mapper() {
					@Override
					public <T> Field<T> map(Field<T> original) {
						String originalName = original.getName();
						if (originalName.equals(fieldId.getName())) {
							return renameField(original, original.getUnqualifiedName());
						}
						Name innerName = name("inner_" + originalName);
						return renameField(original, innerName);
					}
				};
				WithFields innerFields = new WithFields(new MappedPunishmentFields(usingFields, innerFieldName));
				Ordering ordering = innerFields.buildOrdering();
				selectQuery = parameters.context
						.select(outerColumns)
						.from(innerQuery)
						.where(ordering.predicate)
						.orderBy(ordering.byFields)
						.offset(skipField)
						.limit(limitField);
			} else {
				List<Field<?>> allColumns = new ArrayList<>(victimColumns);
				simplified.getRegularColumns(allColumns);
				allColumns.add(fieldId);

				Ordering ordering = simplified.buildOrdering();
				selectQuery = parameters.context
						.select(allColumns)
						.from(usingTable)
						.where(simplified.regularPredication().and(victimPredication).and(ordering.predicate))
						.orderBy(ordering.byFields)
						.offset(skipField)
						.limit(limitField);
			}
			return new Query<>(selectQuery, qualified::mapRecord);
		}

	}

	private static <F> Field<F> renameField(Field<F> original, Name newName) {
		// Return field with same type + binding + converter, but strip qualifying table
		@SuppressWarnings("unchecked")
		Binding<Object, F> binding = (Binding<Object, F>) original.getBinding();
		Field<?> renamed = field(newName, original.getDataType());
		@SuppressWarnings("unchecked")
		Field<Object> eraseRenamed = (Field<Object>) renamed;
		Field<F> rebound = eraseRenamed.convert(binding);
		assert rebound.getDataType() == original.getDataType() : "data type mismatch";
		assert rebound.getConverter().getClass() == original.getConverter().getClass() : "converter mismatch";
		assert rebound.getBinding().getClass() == original.getBinding().getClass() : "binding mismatch";
		return rebound;
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
						   Instant currentTime, SortPunishments...ordering) {}

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
				new QueryParameters(context, 1, Instant.EPOCH, SortPunishments.LATEST_END_DATE_FIRST)
		).renderSQL();
	}

	/**
	 * Visible for internal use, for the efficiency of reusing a database connection
	 * during execution of incoming logins
	 *
	 * @param context the database access
	 * @param currentTime the current time snapshot
	 * @param prioritization sorting prioritization
	 * @return finds a single punishment from this selection
	 */
	public Punishment findFirstSpecificPunishment(DSLContext context, Instant currentTime,
												  SortPunishments...prioritization) {
		return requestQuery(
				new QueryParameters(context, 1, currentTime, prioritization)
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
					return findFirstSpecificPunishment(context, resources.time().currentTimestamp(), prioritization);
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
						resources.time().currentTimestamp(),
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
							resources.time().currentTimestamp()
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
