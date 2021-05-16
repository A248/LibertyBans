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

package space.arim.libertybans.it.test.database;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import space.arim.libertybans.it.util.ContextClassLoaderAction;

import java.util.stream.Stream;

public final class ContextClassLoaderArgumentsProvider implements ArgumentsProvider {

	@Override
	public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
		class EmptyClassLoader extends ClassLoader {
			EmptyClassLoader() {
				super(null);
			}
		}
		Stream<ClassLoader> classLoaderStream = Stream.of(new EmptyClassLoader(), getClass().getClassLoader());
		Class<?> parameterType = extensionContext.getRequiredTestMethod().getParameterTypes()[0];
		if (parameterType.equals(ContextClassLoaderAction.class)) {
			return classLoaderStream.map(ContextClassLoaderAction::new).map(Arguments::of);
		}
		if (parameterType.equals(ClassLoader.class)) {
			return classLoaderStream.map(Arguments::of);
		}
		return Stream.empty();
	}

}
