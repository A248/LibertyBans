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

import org.jooq.Record2;
import org.jooq.Table;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.core.schema.tables.Bans;
import space.arim.libertybans.core.schema.tables.Mutes;
import space.arim.libertybans.core.schema.tables.Warns;

import java.util.Objects;

public final class TableForType {

	private final PunishmentType type;

	public TableForType(PunishmentType type) {
		this.type = Objects.requireNonNull(type, "type");
	}

	public Table<? extends Record2<Long, Integer>> dataTable() {
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
}
