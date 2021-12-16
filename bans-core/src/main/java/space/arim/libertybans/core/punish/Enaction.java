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

package space.arim.libertybans.core.punish;

import org.jooq.DSLContext;
import org.jooq.Field;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.database.execute.Transaction;
import space.arim.libertybans.core.database.sql.FixedVictimData;
import space.arim.libertybans.core.database.sql.SequenceValue;
import space.arim.libertybans.core.database.sql.SerializedVictim;
import space.arim.libertybans.core.database.sql.TableForType;
import space.arim.libertybans.core.database.sql.VictimCondition;
import space.arim.libertybans.core.database.sql.VictimData;
import space.arim.libertybans.core.database.sql.VictimTableFields;

import java.time.Instant;

import static java.util.Objects.requireNonNull;
import static org.jooq.impl.DSL.val;
import static space.arim.libertybans.core.schema.Sequences.LIBERTYBANS_PUNISHMENT_IDS;
import static space.arim.libertybans.core.schema.Sequences.LIBERTYBANS_VICTIM_IDS;
import static space.arim.libertybans.core.schema.tables.History.HISTORY;
import static space.arim.libertybans.core.schema.tables.Punishments.PUNISHMENTS;
import static space.arim.libertybans.core.schema.tables.Victims.VICTIMS;

public class Enaction {

	private final OrderDetails orderDetails;
	private final PunishmentCreator creator;

	public Enaction(OrderDetails orderDetails, PunishmentCreator creator) {
		this.orderDetails = orderDetails;
		this.creator = creator;
	}

	public OrderDetails orderDetails() {
		return orderDetails;
	}

	public Punishment enactActive(DSLContext context, Transaction transaction) {
		return enact(context, requireNonNull(transaction, "transaction"), true);
	}

	public Punishment enactHistorical(DSLContext context) {
		return enact(context, null, false);
	}

	private Punishment enact(DSLContext context, Transaction transaction, boolean active) {

		final PunishmentType type = orderDetails.type();
		final Victim victim = orderDetails.victim();
		final Operator operator = orderDetails.operator();
		final String reason = orderDetails.reason();
		final ServerScope scope = orderDetails.scope();
		final Instant start = orderDetails.start();
		final Instant end = orderDetails.end();

		SequenceValue<Long> punishmentIdSequence = new SequenceValue<>(LIBERTYBANS_PUNISHMENT_IDS);
		SequenceValue<Integer> victimIdSequence = new SequenceValue<>(LIBERTYBANS_VICTIM_IDS);
		context
				.insertInto(PUNISHMENTS)
				.columns(
						PUNISHMENTS.ID, PUNISHMENTS.TYPE,
						PUNISHMENTS.OPERATOR, PUNISHMENTS.REASON,
						PUNISHMENTS.SCOPE, PUNISHMENTS.START, PUNISHMENTS.END)
				.values(
						punishmentIdSequence.nextValue(context), val(type, PUNISHMENTS.TYPE),
						val(operator, PUNISHMENTS.OPERATOR), val(reason, PUNISHMENTS.REASON),
						val(scope, PUNISHMENTS.SCOPE), val(start, PUNISHMENTS.START), val(end, PUNISHMENTS.END))
				.execute();

		Field<Long> punishmentIdField = punishmentIdSequence.lastValueInSession(context);
		Field<Integer> victimIdField;
		{
			VictimData victimData = FixedVictimData.from(new SerializedVictim(victim));
			Integer existingVictimId = context
					.select(VICTIMS.ID)
					.from(VICTIMS)
					.where(new VictimCondition(new VictimTableFields()).matchesVictim(victimData))
					.fetchOne(VICTIMS.ID);
			if (existingVictimId == null) {
				context
						.insertInto(VICTIMS)
						.columns(VICTIMS.ID, VICTIMS.TYPE, VICTIMS.UUID, VICTIMS.ADDRESS)
						.values(
								victimIdSequence.nextValue(context),
								val(victimData.type(), VICTIMS.TYPE),
								val(victimData.uuid(), VICTIMS.UUID),
								val(victimData.address(), VICTIMS.ADDRESS)
						)
						.execute();
				victimIdField = victimIdSequence.lastValueInSession(context);
			} else {
				victimIdField = val(existingVictimId, VICTIMS.ID);
			}
		}
		if (active && type != PunishmentType.KICK) {
			var table = new TableForType(type).dataTable();
			var tableFields = table.newRecord();
			if (type.isSingular()) {
				int updateCount = context
						.insertInto(table)
						.columns(tableFields.field1(), tableFields.field2())
						.values(punishmentIdField, victimIdField)
						.onDuplicateKeyIgnore()
						.execute();
				if (updateCount == 0) {
					// There is already a punishment of this type for this victim
					transaction.rollback();
					return null;
				}
			} else {
				context
						.insertInto(table)
						.columns(tableFields.field1(), tableFields.field2())
						.values(punishmentIdField, victimIdField)
						.execute();
			}
		}
		context
				.insertInto(HISTORY)
				.columns(HISTORY.ID, HISTORY.VICTIM)
				.values(punishmentIdField, victimIdField)
				.execute();
		long id = context
				.select(punishmentIdField)
				.fetchSingle()
				.value1();
		Punishment punishment = creator.createPunishment(id, type, victim, operator, reason, scope, start, end);
		if (punishment == null) { // Shouldn't happen
			throw new IllegalStateException("Internal error: Unable to create punishment for id " + id);
		}
		return punishment;
	}

	public static final class OrderDetails {

		private final PunishmentType type;
		private final Victim victim;
		private final Operator operator;
		private final String reason;
		private final ServerScope scope;
		private final Instant start;
		private final Instant end;

		public OrderDetails(PunishmentType type, Victim victim, Operator operator,
							String reason, ServerScope scope, Instant start, Instant end) {
			this.type = requireNonNull(type, "type");
			this.victim = requireNonNull(victim, "victim");
			this.operator = requireNonNull(operator, "operator");
			this.reason = requireNonNull(reason, "reason");
			this.scope = requireNonNull(scope, "scope");
			this.start = requireNonNull(start, "start");
			this.end = requireNonNull(end, "end");
		}

		public PunishmentType type() {
			return type;
		}

		public Victim victim() {
			return victim;
		}

		public Operator operator() {
			return operator;
		}

		public String reason() {
			return reason;
		}

		public ServerScope scope() {
			return scope;
		}

		public Instant start() {
			return start;
		}

		public Instant end() {
			return end;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			OrderDetails that = (OrderDetails) o;
			return type == that.type
					&& victim.equals(that.victim) && operator.equals(that.operator)
					&& reason.equals(that.reason) && scope.equals(that.scope)
					&& start.equals(that.start) && end.equals(that.end);
		}

		@Override
		public int hashCode() {
			int result = type.hashCode();
			result = 31 * result + victim.hashCode();
			result = 31 * result + operator.hashCode();
			result = 31 * result + reason.hashCode();
			result = 31 * result + scope.hashCode();
			result = 31 * result + start.hashCode();
			result = 31 * result + end.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return "Enaction.OrderDetails{" +
					"type=" + type +
					", victim=" + victim +
					", operator=" + operator +
					", reason='" + reason + '\'' +
					", scope=" + scope +
					", start=" + start +
					", end=" + end +
					'}';
		}
	}

	@Override
	public String toString() {
		return "Enaction{" +
				"orderDetails=" + orderDetails +
				", creator=" + creator +
				'}';
	}
}
