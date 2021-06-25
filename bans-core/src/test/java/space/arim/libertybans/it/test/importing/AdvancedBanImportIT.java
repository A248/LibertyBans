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
import space.arim.libertybans.core.scope.ScopeImpl;
import space.arim.libertybans.core.uuid.ServerType;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetServerType;
import space.arim.libertybans.it.SetTime;
import space.arim.omnibus.util.UUIDUtil;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(LocalDatabaseSetup.class)
@LocalDatabaseSetup.Hsqldb
public class AdvancedBanImportIT {

	private final ImportExecutor importExecutor;
	private PluginDatabaseSetup pluginDatabaseSetup;

	@Inject
	public AdvancedBanImportIT(ImportExecutor importExecutor) {
		this.importExecutor = importExecutor;
	}

	@BeforeEach
	public void setup(@DontInject ConnectionSource connectionSource) {
		PluginDatabaseSetup pluginDatabaseSetup = new PluginDatabaseSetup(connectionSource);
		pluginDatabaseSetup.initAdvancedBanSchema();
		this.pluginDatabaseSetup = pluginDatabaseSetup;
	}

	private ScopeManager createScopeManager() {
		ScopeManager scopeManager = mock(ScopeManager.class);
		when(scopeManager.globalScope()).thenReturn(ScopeImpl.GLOBAL);
		return scopeManager;
	}

	private void importFrom(String dataFile, ImportStatistics expectedStatistics) {
		pluginDatabaseSetup.runSqlFromResource("import-data/advancedban/" + dataFile + ".sql");
		ScopeManager scopeManager = createScopeManager();
		ImportSource importSource = pluginDatabaseSetup.createAdvancedBanImportSource(scopeManager);
		CompletableFuture<ImportStatistics> futureImport = importExecutor.performImport(importSource);
		assertDoesNotThrow(futureImport::join, "Import failed: error");
		assertEquals(expectedStatistics, futureImport.join(),
				"Import failed: unexpected import statistics");
	}

	@TestTemplate
	@SetTime(unixTime = 1622852000)
	@SetServerType(ServerType.OFFLINE)
	public void sampleOneOffline() {
		importFrom("sample-one-offline", new ImportStatistics(103, 332, 0));
	}

	@TestTemplate
	@SetTime(unixTime = 1622852000)
	@SetServerType(ServerType.ONLINE)
	public void sampleTwoOnline(UUIDManager uuidManager) {
		uuidManager.addCache(UUIDUtil.fromShortString("840d1667a0e24934a3bd1a7ebbbc0732"), "Cxleos");
		uuidManager.addCache(UUIDUtil.fromShortString("ed5f12cd600745d9a4b9940524ddaecf"), "Aerodactyl_");
		uuidManager.addCache(UUIDUtil.fromShortString("db2bc8bc1aaf4b8b9d56df3793045951"), "GooseLaugh");
		importFrom("sample-two-online", new ImportStatistics(83, 109, 189));
	}
}
