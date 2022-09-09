/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.bootstrap;

import java.util.Set;

final class FilteringClassLoader extends ClassLoader {

	private static final ClassNotFoundException FAILED_FILTER;
	private static final ClassNotFoundException DOES_NOT_LOAD_CLASSES;

	static {
		ClassLoader.registerAsParallelCapable();
		FAILED_FILTER = new ClassNotFoundException(
				"Class is filtered from the eyes of child classloaders");
		DOES_NOT_LOAD_CLASSES = new ClassNotFoundException(
				FilteringClassLoader.class.getName() + " does not itself load classes");
	}

	private final Set<ProtectedLibrary> protectedLibraries;

	FilteringClassLoader(ClassLoader parent, Set<ProtectedLibrary> protectedLibraries) {
		super(parent);
		this.protectedLibraries = Set.copyOf(protectedLibraries);
	}

	@Override
	protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
		for (ProtectedLibrary protectedLibrary : protectedLibraries) {
			if (className.startsWith(protectedLibrary.basePackage())) {
				throw FAILED_FILTER;
			}
		}
		return super.loadClass(className, resolve);
	}

	@Override
	public Class<?> findClass(String className) throws ClassNotFoundException {
		throw DOES_NOT_LOAD_CLASSES;
	}
}
