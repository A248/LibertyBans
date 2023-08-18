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

public final class V34__Scope_identifier_sequence extends BaseJavaMigration {

	@Override
	public void migrate(Context flywayContext) throws Exception {
		MigrationState migrationState = MigrationState.retrieveState(flywayContext);
		Connection connection = flywayContext.getConnection();
		DSLContext context = migrationState.createJooqContext(connection);

		try (Statement statement = connection.createStatement()) {
			SequenceDefinition.integer("scope_ids", 1)
					.defineUsing(statement, context.family());
		}
	}

}
