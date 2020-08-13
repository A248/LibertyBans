/* 
 * LibertyBans-bootstrap
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-bootstrap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-bootstrap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-bootstrap. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import space.arim.libertybans.bootstrap.depend.BootstrapLauncher;
import space.arim.libertybans.bootstrap.depend.Dependency;
import space.arim.libertybans.bootstrap.depend.DependencyLoaderBuilder;
import space.arim.libertybans.bootstrap.depend.Repository;

public class LibertyBansLauncher {

	private final BootstrapLauncher launcher;
	private final Function<Class<?>, String> getPluginFor;
	
	private final Repository ARIM_LESSER_GPL3 = new Repository("https://mvn-repo.arim.space/lesser-gpl3");
	private final Repository ARIM_GPL3 = new Repository("https://mvn-repo.arim.space/gpl3");
	private final Repository ARIM_AFFERO_GPL3 = new Repository("https://mvn-repo.arim.space/affero-gpl3");
	
	private final Repository CENTRAL_REPO = new Repository("https://repo.maven.apache.org/maven2");
	
	public LibertyBansLauncher(Path folder, Executor executor, Function<Class<?>, String> getPluginFor) {
		this.getPluginFor = getPluginFor;

		Path libsFolder = folder.resolve("libs");
		DependencyLoaderBuilder apiDepLoader = new DependencyLoaderBuilder()
				.setExecutor(executor).setOutputDirectory(libsFolder.resolve("api"));
		DependencyLoaderBuilder internalDepLoader = new DependencyLoaderBuilder()
				.setExecutor(executor).setOutputDirectory(libsFolder.resolve("internal"));
		
		addApiDeps(apiDepLoader);
		addInternalDeps(internalDepLoader);

		launcher = new BootstrapLauncher("LibertyBans@" + hashCode(), getClass().getClassLoader(),
				apiDepLoader.build(), internalDepLoader.build(), this::addUrlsToExternalClassLoader);
	}
	
	protected boolean addUrlsToExternalClassLoader(ClassLoader apiClassLoader, Path[] paths) {
		if (!(apiClassLoader instanceof URLClassLoader)) {
			throw new IllegalStateException(
					"To use the default LibertyBansLauncher, the plugin must be loaded through a URLClassLoader");
		}
		Method addUrlMethod;
		try {
			addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			addUrlMethod.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException | InaccessibleObjectException ex) {
			warn("Failed to attach dependencies to API ClassLoader");
			ex.printStackTrace();
			return false;
		}
		try {
			for (Path path : paths) {
				URL url = path.toUri().toURL();
				addUrlMethod.invoke(apiClassLoader, url);
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | MalformedURLException ex) {
			warn("Failed to attach dependencies to API ClassLoader");
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	private static Class<?> forName(String clazzName) {
		try {
			Class<?> result = Class.forName(clazzName);
			return result;
		} catch (ClassNotFoundException ignored) {
			return null;
		}
	}
	
	private static boolean classExists(String clazzName) {
		return forName(clazzName) != null;
	}
	
	private void warnRelocation(String libName, String clazzName) {
		Class<?> libClass = forName(clazzName);
		if (libClass != null) {
			String plugin = getPluginFor.apply(libClass);
			warn("Plugin '" + ((plugin == null) ? "Unknown" : plugin) + "' has shaded the library '" + libName
					+ "' but did not relocate it. This may or may not pose any problems. "
					+ "Contact the author of this plugin and tell them to relocate their dependencies.");
		}
	}
	
	private Dependency readDependency(String simpleDependencyName) {
		URL url = getClass().getClassLoader().getResource("dependencies/" + simpleDependencyName);
		try (InputStream is = url.openStream()) {

			String[] value = new String(is.readAllBytes(), StandardCharsets.US_ASCII).split("\n");
			return Dependency.of(value[0], value[1], value[2], value[3]);

		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	/*
	 * Dependencies here are publicly exposed, but cannot be relocated, because
	 * they are part of the API.
	 * 
	 */
	private void addApiDeps(DependencyLoaderBuilder loader) {
		if (!classExists("space.arim.omnibus.Omnibus")) {
			loader.addPair(readDependency("omnibus"), ARIM_GPL3);
		}
		if (!classExists("space.arim.uuidvault.api.UUIDVault")) {
			loader.addPair(readDependency("uuidvault"), ARIM_LESSER_GPL3);
		}
	}
	
	/*
	 * Dependencies here are added to the isolated ClassLoader used by
	 * the core implementation. They do not need to be relocated.
	 * 
	 */
	private void addInternalDeps(DependencyLoaderBuilder loader) {
		/*
		 * Since Paper, Waterfall, Sponge, and Velocity include slf4j, it may already be present
		 */
		if (!classExists("org.slf4j.Logger")) {
			loader.addPair(readDependency("slf4j-api"), CENTRAL_REPO);
			loader.addPair(readDependency("slf4j-jdk14"), CENTRAL_REPO);
		}

		// HikariCP
		warnRelocation("HikariCP", "com.zaxxer.hikari.HikariConfig");
		loader.addPair(readDependency("hikaricp"), CENTRAL_REPO);

		// HSQLDB
		warnRelocation("HSQLDB", "org.hsqldb.jdbc.JDBCDriver");
		loader.addPair(readDependency("hsqldb"), CENTRAL_REPO);

		// MariaDB-Connector
		warnRelocation("MariaDB-Connector", "org.mariadb.jdbc.Driver");
		loader.addPair(readDependency("mariadb-connector"), CENTRAL_REPO);

		// Caffeine
		warnRelocation("Caffeine", "com.github.benmanes.caffeine.cache.Caffeine");
		loader.addPair(readDependency("caffeine"), CENTRAL_REPO);

		// ArimAPI
		loader.addPair(readDependency("arimapi"), ARIM_GPL3);

		// Self
		loader.addPair(readDependency("self"), ARIM_AFFERO_GPL3);
	}
	
	// Must use System.err because we do not know whether the platform uses slf4j or JUL
	private void warn(String message) {
		System.err.println("[LibertyBans] " + message);
	}

	public CompletableFuture<ClassLoader> attemptLaunch() {
		return launcher.loadAll().thenApply((downloaded) -> {
			if (!downloaded) {
				return null;
			}
			return launcher.getInternalClassLoader();
		});
	}
	
}
