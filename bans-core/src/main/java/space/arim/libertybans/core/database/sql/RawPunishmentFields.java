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

package space.arim.libertybans.core.database.sql;

import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.Table;

import java.util.Objects;

public final class RawPunishmentFields<R extends Record2<Long, Integer>> implements TableFieldAccessor {

	private final Table<R> dataTable;

	public RawPunishmentFields(Table<R> dataTable) {
		this.dataTable = Objects.requireNonNull(dataTable, "dataTable");
	}

	@Override
	public Table<? extends Record> table() {
		return dataTable;
	}

	public Field<Long> id() {
		return dataTable.newRecord().field1();
	}

	public Field<Integer> victimId() {
		return dataTable.newRecord().field2();
	}

	@Override
	public String toString() {
		return "RawPunishmentFields{" +
				"dataTable=" + dataTable +
				'}';
	}
}
