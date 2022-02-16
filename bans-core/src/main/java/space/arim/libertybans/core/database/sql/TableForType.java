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

package space.arim.libertybans.core.database.sql;

import org.jooq.Record10;
import org.jooq.Record12;
import org.jooq.Record2;
import org.jooq.Table;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.core.schema.tables.ApplicableBans;
import space.arim.libertybans.core.schema.tables.ApplicableMutes;
import space.arim.libertybans.core.schema.tables.ApplicableWarns;
import space.arim.libertybans.core.schema.tables.Bans;
import space.arim.libertybans.core.schema.tables.Mutes;
import space.arim.libertybans.core.schema.tables.SimpleBans;
import space.arim.libertybans.core.schema.tables.SimpleMutes;
import space.arim.libertybans.core.schema.tables.SimpleWarns;
import space.arim.libertybans.core.schema.tables.Warns;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class TableForType {

	private final PunishmentType type;

	public TableForType(PunishmentType type) {
		this.type = Objects.requireNonNull(type, "type");
	}

	public RawPunishmentFields<? extends Record2<Long, Integer>> dataTable() {
		return new RawPunishmentFields<>(dataTable0());
	}

	private Table<? extends Record2<Long, Integer>> dataTable0() {
		switch (type) {
		case BAN:
			return Bans.BANS;
		case MUTE:
			return Mutes.MUTES;
		case WARN:
			return Warns.WARNS;
		case KICK:
			throw new UnsupportedOperationException("Does not exist for kicks");
		default:
			throw MiscUtil.unknownType(type);
		}
	}

	public SimpleViewFields<? extends Record10<
			Long, PunishmentType,
			Victim.VictimType, UUID, NetworkAddress,
			Operator, String, ServerScope, Instant, Instant>> simpleView() {
		return new SimpleViewFields<>(simpleView0());
	}

	private Table<? extends Record10<
			Long, PunishmentType,
			Victim.VictimType, UUID, NetworkAddress,
			Operator, String, ServerScope, Instant, Instant>> simpleView0() {
		switch (type) {
		case BAN:
			return SimpleBans.SIMPLE_BANS;
		case MUTE:
			return SimpleMutes.SIMPLE_MUTES;
		case WARN:
			return SimpleWarns.SIMPLE_WARNS;
		case KICK:
			throw new UnsupportedOperationException("Does not exist for kicks");
		default:
			throw MiscUtil.unknownType(type);
		}
	}

	public ApplicableViewFields<? extends Record12<
			Long, PunishmentType,
			Victim.VictimType, UUID, NetworkAddress,
			Operator, String, ServerScope, Instant, Instant,
			UUID, NetworkAddress>> applicableView() {
		return new ApplicableViewFields<>(applicableView0());
	}

	private Table<? extends Record12<
			Long, PunishmentType,
			Victim.VictimType, UUID, NetworkAddress,
			Operator, String, ServerScope, Instant, Instant,
			UUID, NetworkAddress>> applicableView0() {
		switch (type) {
		case BAN:
			return ApplicableBans.APPLICABLE_BANS;
		case MUTE:
			return ApplicableMutes.APPLICABLE_MUTES;
		case WARN:
			return ApplicableWarns.APPLICABLE_WARNS;
		case KICK:
			throw new UnsupportedOperationException("Does not exist for kicks");
		default:
			throw MiscUtil.unknownType(type);
		}
	}

	@Override
	public String toString() {
		return "TableForType{" +
				"type=" + type +
				'}';
	}
}
