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

import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public final class DatabaseVersion {

	private final DataSource dataSource;

	public DatabaseVersion(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public String obtainVersion() {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData metaData = connection.getMetaData();
			return metaData.getDatabaseProductVersion();
		} catch (SQLException ex) {
			throw new IllegalStateException(
					"Unable to connect to database. Please make sure your MySQL/MariaDB details are correct.", ex);
		}
	}

	void checkVersion(Vendor vendor) {
		if (vendor != Vendor.MARIADB) {
			return;
		}
		String version = obtainVersion();
		if (version.startsWith("10.2") || version.startsWith("5.7")) {
			String warning =
					"\n" +
					"-----------------------------------------------------------\n" +
					"ATTENTION: Your database version is very old:\n" +
					version + "\n" +
					"It is strongly recommended to upgrade your database to the latest version.\n" +
					"\n" +
					"Support for older database versions such as MariaDB 10.2 and MySQL 5.7\n" +
					"will be dropped in the upcoming LibertyBans 1.0.0 release.\n" +
					"\n" +
					"You are responsible for keeping your database up-to-date.\n" +
					"-----------------------------------------------------------\n";
			LoggerFactory.getLogger(getClass()).error(warning);
		}
	}
}
