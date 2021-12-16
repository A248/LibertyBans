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

package space.arim.libertybans.it;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.core.database.Vendor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public enum DatabaseInstance {
	HSQLDB(Vendor.HSQLDB),
	// See pom.xml for where these port properties come from
	MARIADB_LEGACY(Vendor.MARIADB, "libertybans.it.mariadb.legacy.port"),
	MARIADB_MODERN(Vendor.MARIADB, "libertybans.it.mariadb.modern.port"),
	MYSQL(Vendor.MYSQL, "libertybans.it.mysql.port"),
	POSTGRES_LEGACY(Vendor.POSTGRES, "libertybans.it.postgres.legacy.port"),
	POSTGRES_MODERN(Vendor.POSTGRES, "libertybans.it.postgres.modern.port"),
	COCKROACHDB(Vendor.COCKROACH, "libertybans.it.cockroachdb.port"),
	;

	private static final int NO_PORT_APPLICABLE = -1;
	private static final int NO_PORT_CONFIGURED = 0;

	private final Vendor vendor;
	private final int port;

	private static final AtomicInteger DB_NAME_COUNTER = new AtomicInteger();

	DatabaseInstance(Vendor vendor) {
		this.vendor = vendor;
		port = NO_PORT_APPLICABLE;
	}

	DatabaseInstance(Vendor vendor, String portPropertyName) {
		this.vendor = vendor;
		Logger logger = LoggerFactory.getLogger(getClass());
		int port;
		try {
			port = Integer.parseInt(System.getProperty(portPropertyName));
			logger.info("Using database port {} for {}", port, name());
		} catch (NumberFormatException ignored) {
			port = NO_PORT_CONFIGURED;
			logger.info("No database port configured for {}", name());
		}
		this.port = port;
	}

	public Vendor getVendor() {
		return vendor;
	}

	static Stream<DatabaseInstance> matchingVendor(Vendor vendor) {
		return Arrays.stream(DatabaseInstance.values()).filter((dbInstance) -> dbInstance.vendor == vendor);
	}

	Optional<DatabaseInfo> createInfo() {
		switch (port) {
		case NO_PORT_APPLICABLE:
			return Optional.of(new DatabaseInfo());
		case NO_PORT_CONFIGURED:
			return Optional.empty();
		default:
			break;
		}
		String database = "libertybans_it_" + DB_NAME_COUNTER.incrementAndGet();
		createDatabase(database);
		return Optional.of(new DatabaseInfo(port, database));
	}

	private void createDatabase(String database) {
		switch (this) {
		case MARIADB_LEGACY:
		case MARIADB_MODERN:
		case MYSQL:
			createDatabaseUsing("jdbc:mariadb", database, " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
			break;
		case POSTGRES_LEGACY:
		case POSTGRES_MODERN:
		case COCKROACHDB:
			createDatabaseUsing("jdbc:postgresql", database, "");
			break;
		default:
			throw new IllegalStateException("No database creation exists");
		}
	}

	private void createDatabaseUsing(String jdbcPrefix, String database, String characterSet) {
		String user = vendor.userForITs();
		String password = vendor.passwordForITs();
		String jdbcUrl = jdbcPrefix + "://127.0.0.1:" + port + '/';
		try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
			 PreparedStatement prepStmt = conn.prepareStatement(
					 "CREATE DATABASE " + database + characterSet)) {

			prepStmt.execute();
		} catch (SQLException ex) {
			throw new IllegalStateException(
					"Failed to connect to " + jdbcUrl + " with user:password " + user + ':' + password, ex);
		}
	}
}
