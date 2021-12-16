/*
 * LibertyBans
 * Copyright © 2020 Anand Beh
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

package space.arim.libertybans.core.importing;

import space.arim.libertybans.core.config.ReadFromResource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class SqlFromResource {

	private final ConnectionSource connectionSource;

	SqlFromResource(ConnectionSource connectionSource) {
		this.connectionSource = connectionSource;
	}

	void setupAdvancedBan() {
		setupSchema("advancedban");
	}

	void setupLiteBans() {
		setupSchema("litebans");
	}

	private void setupSchema(String schemaName) {
		runSqlFrom("schemas/" + schemaName + ".sql");
	}

	void runSqlFrom(String resource) {
		new ReadFromResource(resource).readBuffered((reader) -> {
			StringBuilder currentQuery = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains(";")) {
					String[] split = line.split(";");

					currentQuery.append(split[0]);
					runQuery(currentQuery);

					StringBuilder nextQuery = new StringBuilder();
					switch (split.length) {
					case 1: // Expected
						break;
					case 2: // Okay
						nextQuery.append(split[1]);
						break;
					default:
						throw new IllegalStateException("Unexpected line " + line);
					}
					currentQuery = nextQuery;
				} else {
					currentQuery.append(line).append('\n');
				}
			}
			String lastQuery = currentQuery.toString();
			if (!lastQuery.isBlank()) {
				runQuery(lastQuery);
			}
			return null;
		});
	}

	private void runQuery(CharSequence query) {
		try (Connection connection = connectionSource.openConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement(query.toString())) {
			preparedStatement.execute();
		} catch (SQLException ex) {
			throw new RuntimeException(ex);
		}
	}
}
