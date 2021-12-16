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

import space.arim.libertybans.core.database.JdbcDriver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public final class JdbcDetails implements ConnectionSource {

	private final String jdbcUrl;
	private final String username;
	private final String password;

	public JdbcDetails(String jdbcUrl, String username, String password) {
		this.jdbcUrl = Objects.requireNonNull(jdbcUrl);
		this.username = username;
		this.password = password;
	}

	@Override
	public Connection openConnection() throws SQLException {
		// Workaround DriverManager not supporting drivers not loaded through system ClassLoader
		if (jdbcUrl.startsWith("jdbc:hsqldb")) {
			initDriver(JdbcDriver.HSQLDB);
		}
		if (jdbcUrl.startsWith("jdbc:mariadb")) {
			initDriver(JdbcDriver.MARIADB_CONNECTOR);
		}
		if (jdbcUrl.startsWith("jdbc:postgresql")) {
			initDriver(JdbcDriver.PG_JDBC);
		}
		return DriverManager.getConnection(jdbcUrl, username, password);
	}

	private void initDriver(JdbcDriver driver) {
		try {
			Class.forName(driver.driverClassName());
		} catch (ClassNotFoundException ex) {
			throw new ImportException("Failed to initialize JDBC driver " + driver, ex);
		}
	}

	@Override
	public String toString() {
		return "JdbcDetails{" +
				"jdbcUrl='" + jdbcUrl + '\'' +
				", username='" + username + '\'' +
				", password='" + password + '\'' +
				'}';
	}
}
