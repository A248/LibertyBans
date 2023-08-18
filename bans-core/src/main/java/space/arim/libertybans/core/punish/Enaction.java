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

package space.arim.libertybans.core.punish;

import org.jooq.DSLContext;
import org.jooq.Field;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.database.execute.Transaction;
import space.arim.libertybans.core.database.sql.ScopeIdSequenceValue;
import space.arim.libertybans.core.database.sql.SequenceValue;
import space.arim.libertybans.core.database.sql.TableForType;
import space.arim.libertybans.core.database.sql.TrackIdSequenceValue;
import space.arim.libertybans.core.database.sql.VictimIdSequenceValue;

import java.time.Instant;

import static java.util.Objects.requireNonNull;
import static org.jooq.impl.DSL.val;
import static space.arim.libertybans.core.schema.Sequences.LIBERTYBANS_PUNISHMENT_IDS;
import static space.arim.libertybans.core.schema.tables.History.HISTORY;
import static space.arim.libertybans.core.schema.tables.Punishments.PUNISHMENTS;

/**
 * The enaction of a punishment. Thread safe.
 */
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
		return orderDetails.enact(creator, context, requireNonNull(transaction, "transaction"), true);
	}

	public Punishment enactHistorical(DSLContext context) {
		return orderDetails.enact(creator, context, null, false);
	}

	public record OrderDetails(
			PunishmentType type, Victim victim, Operator operator, String reason, ServerScope scope,
			Instant start, Instant end, EscalationTrack escalationTrack) {

		public OrderDetails {
			requireNonNull(type, "type");
			requireNonNull(victim, "victim");
			requireNonNull(operator, "operator");
			requireNonNull(reason, "reason");
			requireNonNull(scope, "scope");
			requireNonNull(start, "start");
			requireNonNull(end, "end");
		}

		private Punishment enact(PunishmentCreator creator,
								 DSLContext context, Transaction transaction, boolean active) {
			MiscUtil.checkNoCompositeVictimWildcards(victim);

			Field<Integer> escalationTrackId = new TrackIdSequenceValue(context).retrieveTrackId(escalationTrack);
			Field<Integer> scopeId = new ScopeIdSequenceValue(context).retrieveScopeId(scope);

			SequenceValue<Long> punishmentIdSequence = new SequenceValue<>(context, LIBERTYBANS_PUNISHMENT_IDS);
			context
					.insertInto(PUNISHMENTS)
					.columns(
							PUNISHMENTS.ID, PUNISHMENTS.TYPE, PUNISHMENTS.OPERATOR, PUNISHMENTS.REASON,
							PUNISHMENTS.SCOPE, PUNISHMENTS.START, PUNISHMENTS.END,
							PUNISHMENTS.TRACK, PUNISHMENTS.SCOPE_ID
					)
					.values(
							punishmentIdSequence.nextValue(), val(type, PUNISHMENTS.TYPE),
							val(operator, PUNISHMENTS.OPERATOR), val(reason, PUNISHMENTS.REASON),
							val("", PUNISHMENTS.SCOPE), val(start, PUNISHMENTS.START), val(end, PUNISHMENTS.END),
							escalationTrackId, scopeId
					)
					.execute();

			Field<Long> punishmentIdField = punishmentIdSequence.lastValueInSession();
			Field<Integer> victimIdField = new VictimIdSequenceValue(context).retrieveVictimId(victim);

			if (active && type != PunishmentType.KICK) {
				var dataTable = new TableForType(type).dataTable();
				if (type.isSingular()) {
					int updateCount = context
							.insertInto(dataTable.table())
							.columns(dataTable.id(), dataTable.victimId())
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
							.insertInto(dataTable.table())
							.columns(dataTable.id(), dataTable.victimId())
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
			Punishment punishment = creator.createPunishment(
					id, type, victim, operator, reason, scope, start, end, escalationTrack
			);
			if (punishment == null) { // Shouldn't happen
				throw new IllegalStateException("Internal error: Unable to create punishment for id " + id);
			}
			return punishment;
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
