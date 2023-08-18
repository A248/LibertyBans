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

package space.arim.libertybans.core.database.sql;

import org.jooq.Field;
import org.jooq.Table;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.core.scope.ScopeType;

import java.time.Instant;
import java.util.UUID;

public interface PunishmentFields extends VictimFields, ScopeFields {

	Field<Long> id();

	Field<PunishmentType> type();

	Field<Operator> operator();

	Field<String> reason();

	Field<Instant> start();

	Field<Instant> end();

	Field<EscalationTrack> track();

	default PunishmentFields withNewTable(Table<?> newTable) {
		record ModifiedTable(Field<Long> id, Field<PunishmentType> type, Field<Victim.VictimType> victimType,
							 Field<UUID> victimUuid, Field<NetworkAddress> victimAddress,
							 Field<Operator> operator, Field<String> reason, Field<String> scope,
							 Field<Instant> start, Field<Instant> end, Field<EscalationTrack> track, Field<ScopeType> scopeType,
							 Table<?> table)
				implements PunishmentFields { }
		return new ModifiedTable(
				id(), type(), victimType(), victimUuid(), victimAddress(),
				operator(), reason(), scope(), start(), end(), track(), scopeType(), newTable
		);
	}

}
