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

package space.arim.libertybans.core.importing;

import space.arim.libertybans.core.database.JdbcDriver;

import javax.sql.DataSource;
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
		// Connect manually if possible, for two reasons
		// 1. Workaround DriverManager not supporting drivers not loaded through system ClassLoader
		// 2. Avoid polluting the driver registry if we don't have to. Helps the ecosystem
		for (JdbcDriver driver : JdbcDriver.values()) {
			if (driver.ownsUrl(jdbcUrl)) {
				try {
					return manualConnect(driver);
				} catch (ReflectiveOperationException ex) {
					throw new ImportException("Failed to initialize JDBC data source for " + driver, ex);
				}
			}
		}
		// Scan the global driver registry. This is potentially susceptible to classloading chaos,
		// however, this url was requested for importing purposes, so we must serve the request
		return DriverManager.getConnection(jdbcUrl, username, password);
	}

	private Connection manualConnect(JdbcDriver driver) throws SQLException, ReflectiveOperationException {
		// Instantiate data source
		DataSource dataSource = Class.forName(driver.dataSourceClassName())
				.asSubclass(DataSource.class)
				.getDeclaredConstructor()
				.newInstance();
		// Set url. All implementations have this method
		dataSource.getClass()
				.getMethod("setUrl", String.class)
				.invoke(dataSource, jdbcUrl);
		// Retrieve connection
		return dataSource.getConnection(username, password);
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
