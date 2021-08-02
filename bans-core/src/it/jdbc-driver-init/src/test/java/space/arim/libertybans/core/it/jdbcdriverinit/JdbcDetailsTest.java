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

package space.arim.libertybans.core.it.jdbcdriverinit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import space.arim.libertybans.core.importing.JdbcDetails;

import java.nio.file.Path;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JdbcDetailsTest {

	@TempDir
	public Path databaseFile;

	@BeforeEach
	public void initDriverManager() {
		Thread currentThread = Thread.currentThread();
		ClassLoader originalLoader = currentThread.getContextClassLoader();
		currentThread.setContextClassLoader(new ClassLoader(null) {});
		try {
			Set<Driver> drivers = DriverManager.drivers().collect(Collectors.toUnmodifiableSet());
			assertTrue(drivers.isEmpty(), "Found drivers " + drivers);
		} finally {
			currentThread.setContextClassLoader(originalLoader);
		}
	}

	@Test
	public void openConnection() {
		// Verify the premise of the test is correct
		assertThrows(SQLException.class, () -> {
			DriverManager.getConnection("jdbc:hsqldb:mem:testdb");
		}, "DriverManager contains HSQLDB driver");

		JdbcDetails jdbcDetails = new JdbcDetails("jdbc:hsqldb:file:" + databaseFile, "SA", "");
		assertDoesNotThrow(jdbcDetails::openConnection);
	}
}
