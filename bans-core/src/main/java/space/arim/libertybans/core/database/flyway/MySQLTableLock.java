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
import org.jooq.Sequence;
import org.jooq.Table;
import space.arim.libertybans.core.schema.Sequences;
import space.arim.libertybans.core.schema.tables.Bans;
import space.arim.libertybans.core.schema.tables.History;
import space.arim.libertybans.core.schema.tables.Mutes;
import space.arim.libertybans.core.schema.tables.Warns;

import java.util.Objects;
import java.util.StringJoiner;

import static space.arim.libertybans.core.schema.Tables.*;

final class MySQLTableLock {

	private final MigrationState migrationState;

	MySQLTableLock(MigrationState migrationState) {
		this.migrationState = Objects.requireNonNull(migrationState, "migrationState");
	}

	void lockTables(DSLContext context) {
		/*
		All tables need to be locked. The session can only access locked tables while locks are held.

		The schema_history table needs to be locked because flyway locks it. If we didn't re-lock it,
		LOCK TABLES would implicitly release the lock, which breaks flyway.
		 */
		StringJoiner joinedTables = new StringJoiner(", ");
		var tables = new Table<?>[] {
				REVISION, NAMES, ADDRESSES, PUNISHMENTS, VICTIMS,
				Bans.BANS, Mutes.MUTES, Warns.WARNS, History.HISTORY,
				SIMPLE_ACTIVE, SIMPLE_HISTORY, SCHEMA_HISTORY,
				ZEROEIGHT_NAMES, ZEROEIGHT_ADDRESSES, ZEROEIGHT_PUNISHMENTS,
				ZEROEIGHT_BANS, ZEROEIGHT_MUTES, ZEROEIGHT_WARNS, ZEROEIGHT_HISTORY
		};
		for (Table<?> table : tables) {
			joinedTables.add(migrationState.tablePrefix() + table.getName() + " WRITE");
		}
		for (Sequence<?> sequence : new Sequence[] {Sequences.LIBERTYBANS_PUNISHMENT_IDS, Sequences.LIBERTYBANS_VICTIM_IDS}) {
			joinedTables.add(sequence.getName() + " WRITE");
		}
		context.query("LOCK TABLES " + joinedTables).execute();
	}
}
