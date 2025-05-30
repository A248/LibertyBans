/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

import space.arim.libertybans.bootstrap.logger.BootstrapLogger;

import java.util.Objects;

public interface LibraryDetection {

	boolean evaluatePresence(BootstrapLogger logger);

	static LibraryDetection enabled() {
		return (l) -> true;
	}

	static LibraryDetection eitherOf(LibraryDetection first, LibraryDetection other) {
		class EitherOf implements LibraryDetection {

			@Override
			public boolean evaluatePresence(BootstrapLogger logger) {
				return first.evaluatePresence(logger) || other.evaluatePresence(logger);
			}
		}
		return new EitherOf();
	}

	/**
	 * Searches for the {@code getSLF4JLogger} method on a plugin
	 *
	 */
	class Slf4jPluginLoggerMethod implements LibraryDetection {

		private final Object plugin;

		public Slf4jPluginLoggerMethod(Object plugin) {
			this.plugin = Objects.requireNonNull(plugin, "plugin");
		}

		@Override
		public boolean evaluatePresence(BootstrapLogger logger) {
			Class<?> pluginClass = plugin.getClass();
			try {
				pluginClass.getMethod("getLogger");
			} catch (NoSuchMethodException noSuchMethod) {
				throw new IllegalStateException("Not a plugin: " + plugin, noSuchMethod);
			}
			try {
				pluginClass.getMethod("getSLF4JLogger");
				return true;
			} catch (NoSuchMethodException ignored) {
				return false;
			}
		}
	}

	/**
	 * Detects a library based on its existence on the classpath. Used for dependencies which
	 * are internal to a platform, implying their presence should not be relied upon.
	 *
	 */
	class ByClassLoaderScan implements LibraryDetection {

		private final ProtectedLibrary protectedLibrary;
		private final ClassLoader platformClassLoader;

		public ByClassLoaderScan(ProtectedLibrary protectedLibrary, ClassLoader platformClassLoader) {
			this.protectedLibrary = Objects.requireNonNull(protectedLibrary, "protectedLibrary");
			this.platformClassLoader = Objects.requireNonNull(platformClassLoader, "platformClassLoader");
		}

		@Override
		public boolean evaluatePresence(BootstrapLogger logger) {
			return protectedLibrary.detect(platformClassLoader, logger);
		}
	}

	/**
	 * Detects a library based on simple class existence.
	 */
	class ByClassResolution implements LibraryDetection {

		private final ProtectedLibrary protectedLibrary;

		public ByClassResolution(ProtectedLibrary protectedLibrary) {
			this.protectedLibrary = Objects.requireNonNull(protectedLibrary, "protectedLibrary");
		}

		@Override
		public boolean evaluatePresence(BootstrapLogger logger) {
			try {
				Class.forName(protectedLibrary.sampleClass());
				return true;
			} catch (ClassNotFoundException ex) {
				return false;
			}
		}
	}
}
