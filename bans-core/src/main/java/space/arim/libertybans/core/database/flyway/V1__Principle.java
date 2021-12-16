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

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Statement;

import static space.arim.libertybans.core.schema.tables.ZeroeightPunishments.ZEROEIGHT_PUNISHMENTS;

/**
 * Performs two tasks:
 * - Migrates from LibertyBans 0.8.x if necessary
 * - Creates sequences
 *
 * Both of these operations need be done dynamically and are therefore in Java.
 * Moreover, the latter depends on the former: the punishment ID sequence must
 * start at a value which does not surpass existing 0.8.x punishment IDs.
 *
 */
public final class V1__Principle extends BaseJavaMigration {

	private static final String LIBERTYBANS_08X_FLYWAY_TABLE = "libertybans_flyway";

	@Override
	public void migrate(Context flywayContext) throws Exception {
		MigrationState migrationState = MigrationState.retrieveState(flywayContext);
		Connection connection = flywayContext.getConnection();
		DSLContext context = migrationState.createJooqContext(connection);

		int startingPunishmentId;
		if (new TableExists(LIBERTYBANS_08X_FLYWAY_TABLE).exists(context)) {
			// LibertyBans 0.8.x is found
			Integer flywayVersion = (Integer) context
					.select(DSL.max(DSL.field("version")).as("max_version"))
					.from(LIBERTYBANS_08X_FLYWAY_TABLE)
					.fetchSingle("max_version");
			assert flywayVersion != null;
			if (flywayVersion != 5) {
				throw new IllegalStateException(
						"You must update to the latest 0.8.x version before upgrading to LibertyBans 1.0.0");
			}
			LoggerFactory.getLogger(getClass()).info("Beginning migration of data from 0.8.x to 1.0.0");
			int max08xId = prepareMigrationOfLibertyBans08x(context);
			startingPunishmentId = max08xId + 1;
		} else {
			startingPunishmentId = 1;
		}
		try (Statement statement = connection.createStatement()) {
			if (context.family() == SQLDialect.MYSQL) {
				// MySQL does not support sequences, so we must emulate them using tables
				statement.execute("CREATE TABLE \"libertybans_punishment_ids\" (" +
						"\"value\" BIGINT NOT NULL, \"singleton\" INT NOT NULL UNIQUE)");
				statement.execute("CREATE TABLE \"libertybans_victim_ids\" (" +
						"\"value\" INT NOT NULL, \"singleton\" INT NOT NULL UNIQUE)");
				statement.execute("INSERT INTO \"libertybans_punishment_ids\" VALUES (1, 1)");
				statement.execute("INSERT INTO \"libertybans_victim_ids\" VALUES (" + Integer.MIN_VALUE + ", 1)");
			} else {
				// MariaDB uses the non-standard NOCYCLE without a space
				String noCycle = context.family() == SQLDialect.MARIADB ? "NOCYCLE" : "NO CYCLE";
				// HSQLDB requires the standard clause AS <DATATYPE>
				boolean hsqldb = context.family() == SQLDialect.HSQLDB;
				// MariaDB's maximum permitted sequence is this number
				long maxPunishmentId = Long.MAX_VALUE - 1;
				statement.execute(
						"CREATE SEQUENCE \"libertybans_punishment_ids\" " +
								((hsqldb) ? "AS BIGINT " : "") +
								"START WITH " + startingPunishmentId + " " +
								"MINVALUE 1 " +
								"MAXVALUE " + maxPunishmentId + " " +
								noCycle);
				statement.execute(
						"CREATE SEQUENCE \"libertybans_victim_ids\" " +
								"START WITH " + Integer.MIN_VALUE + " " +
								"MINVALUE " + Integer.MIN_VALUE + " " +
								"MAXVALUE " + Integer.MAX_VALUE + " " +
								noCycle);
			}
		}
	}

	private int prepareMigrationOfLibertyBans08x(DSLContext context) {
		context
				.dropTable("libertybans_revision")
				.execute();
		context
				.alterTable(LIBERTYBANS_08X_FLYWAY_TABLE)
				.renameTo("libertybans_zeroeight_flyway")
				.execute();

		new Rename08xTables(
				"libertybans_",
				(table) -> "libertybans_zeroeight_" + table
		).rename(context);

		Integer maxId = (Integer) context
				.select(DSL.max(ZEROEIGHT_PUNISHMENTS.ID).as("max_id"))
				.from(ZEROEIGHT_PUNISHMENTS)
				.fetchSingle("max_id");
		assert maxId != null;
		return maxId;
	}
}
