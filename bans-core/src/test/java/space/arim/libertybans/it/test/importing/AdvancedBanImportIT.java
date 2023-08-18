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
import space.arim.libertybans.core.uuid.ServerType;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.PlatformSpecs;
import space.arim.libertybans.it.SetServerType;
import space.arim.libertybans.it.SetTime;
import space.arim.omnibus.util.UUIDUtil;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(LocalDatabaseSetup.class)
@LocalDatabaseSetup.Hsqldb
public class AdvancedBanImportIT {

	private final ImportExecutor importExecutor;
	private final ScopeManager scopeManager;
	private final UUIDManager uuidManager;
	private PluginDatabaseSetup pluginDatabaseSetup;

	@Inject
	public AdvancedBanImportIT(ImportExecutor importExecutor, ScopeManager scopeManager, UUIDManager uuidManager) {
		this.importExecutor = importExecutor;
		this.scopeManager = scopeManager;
		this.uuidManager = uuidManager;
	}

	@BeforeEach
	public void setup(@DontInject ConnectionSource connectionSource) {
		PluginDatabaseSetup pluginDatabaseSetup = new PluginDatabaseSetup(connectionSource);
		pluginDatabaseSetup.initAdvancedBanSchema();
		this.pluginDatabaseSetup = pluginDatabaseSetup;
	}

	private void importFrom(String dataFile, ImportStatistics expectedStatistics) {
		pluginDatabaseSetup.runSqlFromResource("import-data/advancedban/" + dataFile + ".sql");
		ImportSource importSource = pluginDatabaseSetup.createAdvancedBanImportSource(scopeManager);
		CompletableFuture<ImportStatistics> futureImport = importExecutor.performImport(importSource);
		assertDoesNotThrow(futureImport::join, "Import failed: error");
		assertEquals(expectedStatistics, futureImport.join(),
				"Import failed: unexpected import statistics");
	}

	@TestTemplate
	@SetTime(unixTime = SetTime.DEFAULT_TIME)
	@PlatformSpecs(serverTypes = @PlatformSpecs.ServerTypes(ServerType.OFFLINE))
	public void sampleOneOffline() {
		importFrom("sample-one-offline", new ImportStatistics(103, 332, 768));
	}

	@TestTemplate
	@SetTime(unixTime = SetTime.DEFAULT_TIME)
	@PlatformSpecs(serverTypes = @PlatformSpecs.ServerTypes(ServerType.ONLINE))
	public void sampleTwoOnline() {
		addToCache("ed5f12cd600745d9a4b9940524ddaecf", "A248", "Aerodactyl_");
		addToCache("840d1667a0e24934a3bd1a7ebbbc0732", "Cxleos");
		addToCache("00140fe8de0841e9ab79e53b9f3f0fbd", "Ecotastic");
		addToCache("db2bc8bc1aaf4b8b9d56df3793045951", "GooseLaugh");
		addToCache("4b5d2e63db0a4c9a99d9ed3797d12bb6", "innr", "plxyer");
		addToCache("1a49201da06a4e459877c5aa5493a16a", "jecode");

		importFrom("sample-two-online", new ImportStatistics(78, 104, 290));
	}

	private void addToCache(String uuidString, String username, String...extraUsernames) {
		UUID uuid = UUIDUtil.fromShortString(uuidString);
		uuidManager.addCache(uuid, username);
		for (String extraUsername : extraUsernames) {
			uuidManager.addCache(uuid, extraUsername);
		}
	}
}
