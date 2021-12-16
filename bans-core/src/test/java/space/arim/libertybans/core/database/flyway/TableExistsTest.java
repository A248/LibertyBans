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
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.core.importing.ConnectionSource;
import space.arim.libertybans.core.importing.LocalDatabaseSetup;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(LocalDatabaseSetup.class)
@LocalDatabaseSetup.Hsqldb
public class TableExistsTest {

	@Test
	public void exists(ConnectionSource connectionSource) throws SQLException {
		try (Connection connection = connectionSource.openConnection()) {
			DSLContext context = DSL.using(connection, SQLDialect.HSQLDB);

			assertFalse(new TableExists("mytable").exists(context));
			try (Statement statement = connection.createStatement()) {
				statement.execute("CREATE TABLE mytable (id INT NOT NULL)");
			}
			assertTrue(new TableExists("mytable").exists(context));
			assertFalse(new TableExists("myothertable").exists(context));
			assertFalse(new TableExists("mytablesimilar").exists(context));
		}
	}
}
