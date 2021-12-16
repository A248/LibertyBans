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

package space.arim.libertybans.it.util;

import org.flywaydb.core.internal.database.DatabaseType;
import org.flywaydb.core.internal.database.DatabaseTypeRegister;
import org.flywaydb.core.internal.plugin.PluginRegister;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;

public final class FlywayStaticStateManagementExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		// Clear state in PluginRegister
		Field hasRegisteredPlugins = PluginRegister.class.getDeclaredField("hasRegisteredPlugins");
		hasRegisteredPlugins.setAccessible(true);
		synchronized (PluginRegister.REGISTERED_PLUGINS) {
			PluginRegister.REGISTERED_PLUGINS.clear();
			hasRegisteredPlugins.setBoolean(null, false);
		}
	}

	@Override
	public void afterEach(ExtensionContext extensionContext) throws Exception {
		PluginRegister.registerPlugins();
		reinitializeDatabaseTypes();
	}

	public void reinitializeDatabaseTypes() throws Exception {
		Field sortedDatabaseTypesField = DatabaseTypeRegister.class.getDeclaredField("SORTED_DATABASE_TYPES");
		sortedDatabaseTypesField.setAccessible(true);
		@SuppressWarnings("unchecked")
		List<DatabaseType> sortedDatabaseTypes = (List<DatabaseType>) sortedDatabaseTypesField.get(null);

		sortedDatabaseTypes.clear();
		sortedDatabaseTypes.addAll(
				// Copied from Flyway/PluginRegister
				PluginRegister.getPlugins(DatabaseType.class).stream().sorted().collect(Collectors.toList())
		);
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return parameterContext.getParameter().getType() == getClass();
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return this;
	}
}
