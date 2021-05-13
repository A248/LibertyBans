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

package space.arim.libertybans.kyoribundle;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Isolated classloader used to assist in determining whether classes are loaded. <br>
 * <br>
 * The classloader is implemented in an isolated fashion such that it will not rely
 * on whether classes are loaded in the parent class loader.
 */
class HelperClassLoader extends ClassLoader {

	private final ClassLoader parent;

	HelperClassLoader(ClassLoader parent) {
		this.parent = parent;
	}

	HelperClassLoader() {
		this(HelperClassLoader.class.getClassLoader());
	}

	void assertHasClass(String className) {
		assertNotNull(findLoadedClass(className), "Class " + className + " should be present");
	}

	void assertNotHasClass(String className) {
		assertNull(findLoadedClass(className), "Class " + className + " should NOT be present");
	}

	/*
	 * This implementation ensures that loaded classes are defined to this classloader
	 */
	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		// Find existing class
		Class<?> loaded = findLoadedClass(name);
		if (loaded != null) {
			return loaded;
		}
		// If the package name starts with any of these, delegate directly to the parent classloader
		String[] ignorePackages = new String[] {"java", "javax", "sun", "org.bukkit", "org.spigotmc", "net.md_5"};
		for (String ignorePackage : ignorePackages) {
			if (name.startsWith(ignorePackage)) {
				return parent.loadClass(name);
			}
		}
		String classLocation = name.replace('.', '/') + ".class";
		URL url = parent.getResource(classLocation);
		if (url == null) {
			throw new ClassNotFoundException("Class at " + classLocation + " not found");
		}
		byte[] classBytes;
		try (InputStream classStream = url.openStream()) {
			classBytes = classStream.readAllBytes();
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return super.defineClass(name, classBytes, 0, classBytes.length);
	}

	/*
	 * Reflection helpers
	 */

	Class<?> classForName(String className) {
		try {
			return Class.forName(className, false, this);
		} catch (ClassNotFoundException ex) {
			throw Assertions.<RuntimeException>fail(ex);
		}
	}

	Class<?> translateClass(Class<?> clazz) {
		if (clazz.getClassLoader() == this) {
			return clazz;
		}
		return classForName(clazz.getName());
	}

	Object callStaticMethod(Class<?> clazz, String method) {
		try {
			return translateClass(clazz).getMethod(method).invoke(null);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
			throw Assertions.<RuntimeException>fail(ex);
		}
	}

	Object callStaticMethod(String className, String method) {
		Class<?> clazz = classForName(className);
		return callStaticMethod(clazz, method);
	}

	Object constructObject(Class<?> clazz) {
		try {
			return translateClass(clazz).getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
			throw Assertions.<RuntimeException>fail(ex);
		}
	}

}
