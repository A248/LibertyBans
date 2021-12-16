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

package space.arim.libertybans.core.database.flyway;

import org.jooq.DSLContext;
import org.jooq.Table;
import space.arim.libertybans.core.database.Vendor;
import space.arim.libertybans.core.punish.MiscUtil;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

final class TableLock {

	private final MigrationState migrationState;
	private final List<Table<?>> tables;

	TableLock(MigrationState migrationState, List<Table<?>> tables) {
		this.migrationState = Objects.requireNonNull(migrationState, "migrationState");
		this.tables = List.copyOf(tables);
	}

	LockedTables lockTables(DSLContext context) {
		class AutoUnlock implements LockedTables {

			@Override
			public void unlock() {}
		}
		StringJoiner joinedTables = new StringJoiner(", ");
		Vendor vendor = migrationState.vendor();
		switch (vendor) {
		case HSQLDB:
			for (Table<?> table : tables) {
				joinedTables.add(migrationState.tablePrefix() + table.getName() + " WRITE");
			}
			context.query("LOCK TABLES " + joinedTables).execute();
			return new AutoUnlock();
		case MARIADB:
		case MYSQL:
			for (Table<?> table : tables) {
				joinedTables.add(migrationState.tablePrefix() + table.getName() + " WRITE");
			}
			context.query("LOCK TABLES " + joinedTables).execute();
			class MySQLUnlock implements LockedTables {
				@Override
				public void unlock() {
					context.query("UNLOCK TABLES").execute();
				}
			}
			return new MySQLUnlock();
		case POSTGRES:
			for (Table<?> table : tables) {
				joinedTables.add(migrationState.tablePrefix() + table.getName());
			}
			context.query("LOCK TABLES " + joinedTables + " IN EXCLUSIVE MODE").execute();
			return new AutoUnlock(); // PostgreSQL automatically releases locks at transaction end
		case COCKROACH:
			for (Table<?> table : tables) {
				context.selectFrom(table).forUpdate().execute();
			}
			return new AutoUnlock();
		default:
			throw MiscUtil.unknownVendor(vendor);
		}
	}

	interface LockedTables {

		void unlock();

	}
}
