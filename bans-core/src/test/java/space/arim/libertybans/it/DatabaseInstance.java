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

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.core.database.Vendor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public enum DatabaseInstance {
	// See pom.xml for where these port properties come from
	MARIADB_LEGACY("libertybans.it.mariadb.legacy.port"),
	MARIADB_MODERN("libertybans.it.mariadb.modern.port"),
	HSQLDB
	;

	private static final int NO_PORT_APPLICABLE = -1;
	private static final int NO_PORT_CONFIGURED = 0;
	private final int port;

	private static final AtomicInteger DB_NAME_COUNTER = new AtomicInteger();

	DatabaseInstance() {
		port = NO_PORT_APPLICABLE;
	}

	DatabaseInstance(String portPropertyName) {
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

	static Stream<DatabaseInstance> fromVendor(Vendor vendor) {
		switch (vendor) {
		case HSQLDB:
			return Stream.of(DatabaseInstance.HSQLDB);
		case MARIADB:
			return Arrays.stream(DatabaseInstance.values())
					.filter((dbInstance) -> {
						String name = dbInstance.name().toLowerCase(Locale.ROOT);
						return dbInstance.port != NO_PORT_CONFIGURED && name.contains("mariadb");
					});
		default:
			throw new UnsupportedOperationException("Not implemented for " + vendor);
		}
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
		try (Connection conn = DriverManager.getConnection("jdbc:mariadb://127.0.0.1:" + port + '/', "root", "");
			 PreparedStatement prepStmt = conn.prepareStatement("CREATE DATABASE " + database
					 + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")) {

			prepStmt.execute();
		} catch (SQLException ex) {
			throw Assertions.<RuntimeException>fail(ex);
		}
	}

	// Sleep to prevent infrequent errors arising from connecting to the database too quickly
	static {
		try {
			TimeUnit.SECONDS.sleep(2L);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ExceptionInInitializerError(ex);
		}
	}

}
