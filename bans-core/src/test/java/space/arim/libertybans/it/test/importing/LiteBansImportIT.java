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

package space.arim.libertybans.it.test.importing;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.core.importing.ConnectionSource;
import space.arim.libertybans.core.importing.ImportExecutor;
import space.arim.libertybans.core.importing.ImportSource;
import space.arim.libertybans.core.importing.ImportStatistics;
import space.arim.libertybans.core.importing.LocalDatabaseSetup;
import space.arim.libertybans.core.importing.PluginDatabaseSetup;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetTime;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(LocalDatabaseSetup.class)
@LocalDatabaseSetup.H2
public class LiteBansImportIT {

	private final ImportExecutor importExecutor;
	private final ScopeManager scopeManager;
	private PluginDatabaseSetup pluginDatabaseSetup;

	@Inject
	public LiteBansImportIT(ImportExecutor importExecutor, ScopeManager scopeManager) {
		this.importExecutor = importExecutor;
		this.scopeManager = scopeManager;
	}

	@BeforeEach
	public void setup(@DontInject ConnectionSource connectionSource) {
		this.pluginDatabaseSetup = new PluginDatabaseSetup(connectionSource);
	}

	private void importFrom(String dataFile, ImportStatistics expectedStatistics) {
		pluginDatabaseSetup.runSqlFromResource("import-data/litebans/" + dataFile + ".sql");
		ImportSource importSource = pluginDatabaseSetup.createLiteBansImportSource(scopeManager);
		CompletableFuture<ImportStatistics> futureImport = importExecutor.performImport(importSource);
		assertDoesNotThrow(futureImport::join, "Import failed: error");
		assertEquals(expectedStatistics, futureImport.join(),
				"Import failed: unexpected import statistics");
	}

	@TestTemplate
	@SetTime(unixTime = 1621390560)
	public void sampleOne() {
		pluginDatabaseSetup.initLiteBansSchema();
		importFrom("sample-one", new ImportStatistics(71, 1625, 34236));
	}

	@TestTemplate
	@SetTime(unixTime = 1622836000)
	public void sampleTwo() {
		// This sample already contains the schema definition
		importFrom("sample-two", new ImportStatistics(348, 336, 2087));
	}

}
