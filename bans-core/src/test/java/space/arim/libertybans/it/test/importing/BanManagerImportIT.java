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
import jakarta.inject.Provider;
import org.jooq.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.importing.ConnectionSource;
import space.arim.libertybans.core.importing.ImportExecutor;
import space.arim.libertybans.core.importing.ImportSource;
import space.arim.libertybans.core.importing.ImportStatistics;
import space.arim.libertybans.core.importing.LocalDatabaseSetup;
import space.arim.libertybans.core.importing.PluginDatabaseSetup;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static space.arim.libertybans.core.schema.tables.Addresses.ADDRESSES;
import static space.arim.libertybans.core.schema.tables.Names.NAMES;
import static space.arim.libertybans.core.schema.tables.SimpleActive.SIMPLE_ACTIVE;
import static space.arim.libertybans.core.schema.tables.SimpleHistory.SIMPLE_HISTORY;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith(LocalDatabaseSetup.class)
@LocalDatabaseSetup.H2
public class BanManagerImportIT {

	private final ImportExecutor importExecutor;
	private final Provider<QueryExecutor> queryExecutor;
	private PluginDatabaseSetup pluginDatabaseSetup;

	@Inject
	public BanManagerImportIT(ImportExecutor importExecutor, Provider<QueryExecutor> queryExecutor) {
		this.importExecutor = importExecutor;
		this.queryExecutor = queryExecutor;
	}

	@BeforeEach
	public void setup(@DontInject ConnectionSource connectionSource) {
		this.pluginDatabaseSetup = new PluginDatabaseSetup(connectionSource);
	}

	private int selectCount(Table<?> table) {
		return queryExecutor.get().query((context) -> {
			return context.selectCount().from(table).fetchSingle().value1();
		}).join();
	}

	@TestTemplate
	public void fabricatedData(ScopeManager scopeManager) {
		pluginDatabaseSetup.initBanManagerSchema();
		pluginDatabaseSetup.runSqlFromResource("import-data/banmanager/fabricated-data.sql");
		ImportSource importSource = pluginDatabaseSetup.createBanManagerImportSource(scopeManager);
		ImportStatistics importStatistics = importExecutor.performImport(importSource).join();
		assertEquals(new ImportStatistics(3, 3, 1), importStatistics);

		assertEquals(3, selectCount(SIMPLE_ACTIVE));
		assertEquals(6, selectCount(SIMPLE_HISTORY));
		assertEquals(1, selectCount(NAMES));
		assertEquals(1, selectCount(ADDRESSES));
	}
}
