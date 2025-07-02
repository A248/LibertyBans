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
import space.arim.libertybans.core.schema.tables.Bans;
import space.arim.libertybans.core.schema.tables.Mutes;
import space.arim.libertybans.core.schema.tables.Warns;

public interface RawPunishmentFields extends TableFieldAccessor {
	Field<Long> id();

	Field<Integer> victimId();
}

record RawBansFields(Bans table) implements RawPunishmentFields {

	@Override
	public Field<Long> id() {
		return table.ID;
	}

	@Override
	public Field<Integer> victimId() {
		return table.VICTIM;
	}
}

record RawMutesFields(Mutes table) implements RawPunishmentFields {

	@Override
	public Field<Long> id() {
		return table.ID;
	}

	@Override
	public Field<Integer> victimId() {
		return table.VICTIM;
	}
}

record RawWarnsFields(Warns table) implements RawPunishmentFields {

	@Override
	public Field<Long> id() {
		return table.ID;
	}

	@Override
	public Field<Integer> victimId() {
		return table.VICTIM;
	}
}
