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

package space.arim.libertybans.core.database.sql;

import org.jooq.Field;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.core.schema.tables.SimpleHistory;
import space.arim.libertybans.core.scope.ScopeType;

import java.time.Instant;
import java.util.UUID;

public record SimpleHistoryFields(SimpleHistory table) implements SimpleViewFields {
	@Override
	public Field<Long> id() {
		return table.ID;
	}

	@Override
	public Field<PunishmentType> type() {
		return table.TYPE;
	}

	@Override
	public Field<Operator> operator() {
		return table.OPERATOR;
	}

	@Override
	public Field<String> reason() {
		return table.REASON;
	}

	@Override
	public Field<Instant> start() {
		return table.START;
	}

	@Override
	public Field<Instant> end() {
		return table.END;
	}

	@Override
	public Field<EscalationTrack> track() {
		return table.TRACK;
	}

	@Override
	public Field<String> scope() {
		return table.SCOPE;
	}

	@Override
	public Field<ScopeType> scopeType() {
		return table.SCOPE_TYPE;
	}

	@Override
	public Field<Victim.VictimType> victimType() {
		return table.VICTIM_TYPE;
	}

	@Override
	public Field<UUID> victimUuid() {
		return table.VICTIM_UUID;
	}

	@Override
	public Field<NetworkAddress> victimAddress() {
		return table.VICTIM_ADDRESS;
	}
}
