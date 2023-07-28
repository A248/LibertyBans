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

package space.arim.libertybans.core.database.flyway;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import space.arim.libertybans.core.database.sql.SequenceDefinition;

import java.sql.Connection;
import java.sql.Statement;

import static space.arim.libertybans.core.database.DatabaseConstants.LIBERTYBANS_08X_FLYWAY_TABLE;

/**
 * Performs two tasks: <br>
 * - Migrates from LibertyBans 0.8.x if necessary <br>
 * - Creates sequences <br>
 * <br>
 * Both of these operations need be done dynamically and are therefore in Java.
 * Moreover, the latter depends on the former: the punishment ID sequence must
 * start at a value which does not surpass existing 0.8.x punishment IDs.
 *
 */
public final class V1__Principle extends BaseJavaMigration {

	@Override
	public void migrate(Context flywayContext) throws Exception {
		MigrationState migrationState = MigrationState.retrieveState(flywayContext);
		Connection connection = flywayContext.getConnection();
		DSLContext context = migrationState.createJooqContext(connection);

		if (new TableExists(LIBERTYBANS_08X_FLYWAY_TABLE).exists(context)) {
			// LibertyBans 0.8.x is found
			throw new IllegalStateException(
					"Automatic upgrade from LibertyBans 0.8.x is no longer supported past version 1.1.0"
			);
		}
		try (Statement statement = connection.createStatement()) {
			SequenceDefinition.bigInteger("punishment_ids", 1)
					.defineUsing(statement, context.family());
			SequenceDefinition.integer("victim_ids", Integer.MIN_VALUE)
					.defineUsing(statement, context.family());
		}
	}

}
