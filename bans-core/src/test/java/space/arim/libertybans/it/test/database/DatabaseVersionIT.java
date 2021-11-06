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

package space.arim.libertybans.it.test.database;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.core.database.DatabaseManager;
import space.arim.libertybans.core.database.DatabaseVersion;
import space.arim.libertybans.it.DatabaseInstance;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith(MockitoExtension.class)
public class DatabaseVersionIT {

	private final DataSource dataSource;
	private DatabaseVersion databaseVersion;

	@Inject
	public DatabaseVersionIT(@Mock @DontInject DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@BeforeEach
	public void setDatabaseVersion(DatabaseManager databaseManager) throws SQLException {
		when(dataSource.getConnection()).thenReturn(databaseManager.getInternal().getConnection());
		databaseVersion = new DatabaseVersion(dataSource);
	}

	@TestTemplate
	public void databaseVersion(DatabaseInstance databaseInstance) {
		String expectedVersion;
		switch (databaseInstance) {
		case MARIADB_LEGACY:
			expectedVersion = "10.2";
			break;
		case MARIADB_MODERN:
			expectedVersion = "10.6";
			break;
		default:
			return;
		}
		String actualVersion = databaseVersion.obtainVersion();
		assertTrue(actualVersion.startsWith(expectedVersion), "Expected " + expectedVersion + " but got " + actualVersion);
	}

}
