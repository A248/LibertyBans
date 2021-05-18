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
import space.arim.libertybans.core.importing.LocalDatabaseSetup;
import space.arim.libertybans.core.importing.PluginDatabaseSetup;
import space.arim.libertybans.core.scope.ScopeImpl;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith(MockitoExtension.class)
@ExtendWith(LocalDatabaseSetup.class)
@LocalDatabaseSetup.H2
public class LiteBansImportIT {

	private final ImportExecutor importExecutor;
	private PluginDatabaseSetup pluginDatabaseSetup;

	@Inject
	public LiteBansImportIT(ImportExecutor importExecutor) {
		this.importExecutor = importExecutor;
	}

	@BeforeEach
	public void setup(@DontInject ConnectionSource connectionSource) {
		PluginDatabaseSetup pluginDatabaseSetup = new PluginDatabaseSetup(connectionSource);
		pluginDatabaseSetup.initLiteBansSchema();
		pluginDatabaseSetup.runSqlFromResource("import-data/litebans.sql");
		this.pluginDatabaseSetup = pluginDatabaseSetup;
	}

	private ScopeManager createScopeManager() {
		ScopeManager scopeManager = mock(ScopeManager.class);
		// Specific server scopes not covered by this IT
		when(scopeManager.globalScope()).thenReturn(ScopeImpl.GLOBAL);
		return scopeManager;
	}

	@TestTemplate
	public void importEverything() {
		ScopeManager scopeManager = createScopeManager();
		ImportSource importSource = pluginDatabaseSetup.createLiteBansImportSource(scopeManager);
		CompletableFuture<Boolean> futureImport = importExecutor.performImport(importSource);
		assertDoesNotThrow(futureImport::join, "Import failed: error");
		assertTrue(futureImport.join(), "Import failed: unsuccessful");
	}
}
