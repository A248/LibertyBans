/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.omnibus.util.UUIDUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class PluginDatabaseSetup {

	private final ConnectionSource connectionSource;

	public PluginDatabaseSetup(ConnectionSource connectionSource) {
		this.connectionSource = Objects.requireNonNull(connectionSource);
	}

	public ImportSource createAdvancedBanImportSource(ScopeManager scopeManager) {
		ImportConfig importConfig = mock(ImportConfig.class);
		ImportConfig.AdvancedBanSettings advancedBanSettings = mock(ImportConfig.AdvancedBanSettings.class);
		when(importConfig.retrievalSize()).thenReturn(200);
		when(importConfig.advancedBan()).thenReturn(advancedBanSettings);
		when(advancedBanSettings.toConnectionSource()).thenReturn(connectionSource);

		return new AdvancedBanImportSource(importConfig, scopeManager);
	}

	public ImportSource createLiteBansImportSource(ScopeManager scopeManager) {
		ImportConfig importConfig = mock(ImportConfig.class);
		ImportConfig.LiteBansSettings liteBansSettings = mock(ImportConfig.LiteBansSettings.class);
		when(importConfig.retrievalSize()).thenReturn(200);
		when(importConfig.litebans()).thenReturn(liteBansSettings);
		when(liteBansSettings.tablePrefix()).thenReturn("litebans_");
		when(liteBansSettings.toConnectionSource()).thenReturn(connectionSource);

		return new LiteBansImportSource(importConfig, scopeManager);
	}

	public ImportSource createBanManagerImportSource(ScopeManager scopeManager) {
		ImportConfig importConfig = mock(ImportConfig.class);
		ImportConfig.BanManagerSettings banManagerSettings = mock(ImportConfig.BanManagerSettings.class);
		when(importConfig.retrievalSize()).thenReturn(200);
		when(importConfig.banManager()).thenReturn(banManagerSettings);
		when(banManagerSettings.tablePrefix()).thenReturn("bm_");
		when(banManagerSettings.toConnectionSource()).thenReturn(connectionSource);

		return new BanManagerImportSource(importConfig, scopeManager);
	}

	public void addBanManagerConsole(UUID consoleUuid) {
		String uuidString = UUIDUtil.toShortString(consoleUuid);
		try (Connection connection = connectionSource.openConnection();
			 Statement statement = connection.createStatement()) {

			statement.execute(
					"INSERT INTO \"bm_players\" (\"id\", \"name\", \"ip\", \"lastSeen\") " +
							"VALUES (X'" + uuidString + "', 'Console', X'7f000001', 0)");
		} catch (SQLException ex) {
			throw new IllegalStateException("Unable to set console UUID", ex);
		}
	}

	public void initAdvancedBanSchema() {
		initPluginSchema("advancedban");
	}

	public void initBanManagerSchema() {
		initPluginSchema("banmanager");
	}

	public void initLiteBansSchema() {
		initPluginSchema("litebans");
	}

	private void initPluginSchema(String schemaName) {
		runSqlFromResource("schemas/" + schemaName + ".sql");
	}

	public void runSqlFromResource(String resourceName) {
		new SqlFromResource(connectionSource).runSqlFrom(resourceName);
	}

}
