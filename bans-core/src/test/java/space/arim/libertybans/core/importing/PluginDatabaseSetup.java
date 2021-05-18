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

package space.arim.libertybans.core.importing;

import org.junit.jupiter.api.Assertions;
import space.arim.jdbcaesar.JdbCaesar;
import space.arim.jdbcaesar.JdbCaesarBuilder;
import space.arim.libertybans.api.scope.ScopeManager;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.Objects;

import static org.mockito.Mockito.lenient;
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

	public JdbCaesar createJdbCaesar() {
		DataSource dataSource = mock(DataSource.class);
		try {
			lenient().when(dataSource.getConnection()).thenAnswer((invocation) -> connectionSource.openConnection());
		} catch (SQLException ex) {
			throw Assertions.<RuntimeException>fail(ex);
		}
		return new JdbCaesarBuilder().dataSource(dataSource).build();
	}

	public void initAdvancedBanSchema() {
		new SqlFromResource(connectionSource).setupAdvancedBan();
	}

	public void initLiteBansSchema() {
		new SqlFromResource(connectionSource).setupLiteBans();
	}

	public void runSqlFromResource(String resourceName) {
		new SqlFromResource(connectionSource).runSqlFrom(resourceName);
	}

}
