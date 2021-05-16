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

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.core.config.SqlConfig;
import space.arim.libertybans.core.database.DatabaseManager;
import space.arim.libertybans.core.database.DatabaseResult;
import space.arim.libertybans.core.database.DatabaseSettings;
import space.arim.libertybans.core.database.Vendor;
import space.arim.libertybans.it.util.ContextClassLoaderAction;
import space.arim.libertybans.it.util.FlywayResetStaticStateExtension;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(FlywayResetStaticStateExtension.class)
public class DatabaseSettingsIT {

	@TempDir
	public Path databaseDir;

	@ParameterizedTest
	@ArgumentsSource(ContextClassLoaderArgumentsProvider.class)
	public void migrateWithContextClassLoader(ContextClassLoaderAction contextLoader) {
		contextLoader.assertDoesNotThrowUsingContextLoader(this::startDb);
	}

	private void startDb() {
		DatabaseManager dbManager = mock(DatabaseManager.class);
		when(dbManager.folder()).thenReturn(databaseDir);
		SqlConfig sqlConfig = mock(SqlConfig.class);
		when(sqlConfig.authDetails()).thenReturn(mock(SqlConfig.AuthDetails.class));
		when(sqlConfig.vendor()).thenReturn(Vendor.HSQLDB);
		when(sqlConfig.poolSize()).thenReturn(1);
		when(sqlConfig.useTraditionalJdbcUrl()).thenReturn(false);
		SqlConfig.Timeouts timeouts = mock(SqlConfig.Timeouts.class);
		when(timeouts.connectionTimeoutSeconds()).thenReturn(30);
		when(timeouts.maxLifetimeMinutes()).thenReturn(15);
		when(sqlConfig.timeouts()).thenReturn(timeouts);

		DatabaseResult dbResult = new DatabaseSettings(dbManager).create(sqlConfig);
		assertTrue(dbResult.success(), "Database creation failed");
	}
}
