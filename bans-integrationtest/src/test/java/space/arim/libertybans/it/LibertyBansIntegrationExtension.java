/* 
 * LibertyBans-integrationtest
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-integrationtest is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-integrationtest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-integrationtest. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.it;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import space.arim.libertybans.core.database.InternalDatabase;

import space.arim.injector.Injector;
import space.arim.injector.error.InjectorException;

class LibertyBansIntegrationExtension implements ParameterResolver, AfterEachCallback {

	private final Injector injector;

	LibertyBansIntegrationExtension(Injector injector) {
		this.injector = injector;
	}

	/*
	 * ParameterResolver
	 */

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return !parameterContext.isAnnotated(DontInject.class);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		try {
			return injector.request(parameterContext.getParameter().getType());
		} catch (InjectorException ex) {
			throw new ParameterResolutionException("Unable to inject parameter", ex);
		}
	}

	/*
	 * AfterEachCallback
	 */

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		injector.request(InternalDatabase.class).truncateAllTables();
	}

}
