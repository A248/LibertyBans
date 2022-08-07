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

public final class Platforms {

	private Platforms() {}

	public static boolean detectGetSlf4jLoggerMethod(Object plugin) {
		try {
			plugin.getClass().getMethod("getSLF4JLogger");
			return true;
		} catch (NoSuchMethodException ignored) {
			return false;
		}
	}

	/**
	 * Determines whether a library is present in the given classloader or any of its
	 * parent classloaders
	 *
	 * @param className a class in the library
	 * @param expectedClassLoader the classloader
	 * @return true if the library is present in the classloader or any of its parents
	 */
	public static boolean detectLibrary(String className, ClassLoader expectedClassLoader) {
		Class<?> libClass;
		try {
			libClass = Class.forName(className);
		} catch (ClassNotFoundException ex) {
			return false;
		}
		return findClassLoaderInHierarchy(libClass.getClassLoader(), expectedClassLoader);
	}

	private static boolean findClassLoaderInHierarchy(ClassLoader subjectToLookFor, ClassLoader hierarchy) {
		if (subjectToLookFor == hierarchy) {
			return true;
		}
		ClassLoader parent = hierarchy.getParent();
		return parent != null && findClassLoaderInHierarchy(subjectToLookFor, parent);
	}

	public static Platform velocity(boolean caffeine) {
		return Platform.forCategory(Platform.Category.VELOCITY)
				.slf4jSupport(true).kyoriAdventureSupport(true).caffeineProvided(caffeine)
				.build("Velocity");
	}

}
