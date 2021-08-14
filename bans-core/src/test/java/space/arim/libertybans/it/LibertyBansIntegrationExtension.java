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
package space.arim.libertybans.it;

import jakarta.inject.Provider;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import space.arim.injector.Injector;
import space.arim.injector.error.InjectorException;
import space.arim.libertybans.core.database.InternalDatabase;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

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
		Class<?> parameterType = parameterContext.getParameter().getType();
		try {
			if (parameterType.equals(Provider.class)) {
				Type type = parameterContext.getParameter().getParameterizedType();
				ParameterizedType parameterizedType = (ParameterizedType) type;
				Class<?> typeArgument = (Class<?>) parameterizedType.getActualTypeArguments()[0];
				return (Provider<Object>) () -> injector.request(typeArgument);
			}
			return injector.request(parameterType);
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
