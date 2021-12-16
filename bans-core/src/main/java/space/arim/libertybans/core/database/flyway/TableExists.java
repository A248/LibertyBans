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

import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

final class TableExists {

	private final String tableName;

	TableExists(String tableName) {
		this.tableName = Objects.requireNonNull(tableName, "tableName");
	}

	boolean exists(DSLContext context) throws SQLException {

		ConnectionProvider connectionProvider = context.configuration().connectionProvider();
		Connection connection = connectionProvider.acquire();
		assert connection != null : "Null connection";
		try {
			String currentSchema = context
					.select(DSL.currentSchema())
					.fetchSingle()
					.value1();
			DatabaseMetaData metaData = connection.getMetaData();
			// Be VERY CAREFUL only to select from our own database (schema)
			try (ResultSet tables = metaData.getTables(null, currentSchema, "%", new String[] {"TABLE"})) {

				while (tables.next()) {
					String foundTable = tables.getString("TABLE_NAME");
					if (foundTable.equalsIgnoreCase(tableName)) {
						return true;
					}
				}
			}
			return false;
		} finally {
			connectionProvider.release(connection);
		}
	}
}
