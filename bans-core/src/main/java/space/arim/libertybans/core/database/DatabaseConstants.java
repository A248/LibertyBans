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

package space.arim.libertybans.core.database;

import org.jooq.Table;

import java.time.Duration;

import static space.arim.libertybans.core.schema.Tables.*;

/*
 * Used to consolidate defaults configured across HikariCP and JDBC drivers
 * 
 */
public final class DatabaseConstants {

	static final boolean AUTOCOMMIT = false;
	static final int FETCH_SIZE = 1000;
	static final long SOCKET_TIMEOUT = Duration.ofSeconds(30L).toMillis();

	public static final String LIBERTYBANS_08X_FLYWAY_TABLE = "libertybans_flyway";

	private DatabaseConstants() {}

	/**
	 * Gets all the tables in the specified order
	 *
	 * @param tableOrder the order in which to return the tables
	 * @return the tables in an order which considers foreign key references
	 */
	public static Table<?>[] allTables(TableOrder tableOrder) {
		// Referees first, referents last with respect to foreign keys
		Table<?>[] tables = new Table[] {
				NAMES, ADDRESSES, HISTORY, BANS, MUTES, WARNS, PUNISHMENTS, VICTIMS, MESSAGES, REVISION
		};
		if (tableOrder == TableOrder.REFERENTS_FIRST) {
			// Reverse array
			int midpoint = tables.length / 2;
			for (int n = 0; n < midpoint; n++) {

				int swapIndex = tables.length - n - 1;
				Table<?> subject, swapWith;
				subject = tables[n];
				swapWith = tables[swapIndex];
				tables[n] = swapWith;
				tables[swapIndex] = subject;
			}
		}
		return tables;
	}

	public enum TableOrder {
		/**
		 * Referenced tables are placed first, and tables referring to them come last. <br>
		 * <br>
		 * Commonly used for inserting data
		 */
		REFERENTS_FIRST,
		/**
		 * Referenced tables are placed last, and tables referring to them come first. <br>
		 * <br>
		 * Commonly used for deleting data
		 */
		REFERENTS_LAST
	}

	public static Table<?>[] allViews() {
		return new Table[] {
				LATEST_NAMES, LATEST_ADDRESSES, STRICT_LINKS,
				APPLICABLE_ACTIVE, APPLICABLE_HISTORY, APPLICABLE_BANS, APPLICABLE_MUTES, APPLICABLE_WARNS,
				SIMPLE_ACTIVE, SIMPLE_HISTORY, SIMPLE_BANS, SIMPLE_MUTES, SIMPLE_WARNS
		};
	}
}
