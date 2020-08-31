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
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

import space.arim.libertybans.bootstrap.depend.BootstrapLauncher;
import space.arim.libertybans.bootstrap.depend.Dependency;
import space.arim.libertybans.bootstrap.depend.DependencyLoaderBuilder;

public class LibertyBansLauncher {

	private final Executor executor;
	private final BootstrapLauncher launcher;
	private final Function<Class<?>, String> getPluginFor;
	
	public LibertyBansLauncher(Path folder, Executor executor, Function<Class<?>, String> getPluginFor) {
		this.executor = executor;
		this.getPluginFor = getPluginFor;

		Path libsFolder = folder.resolve("libs");
		DependencyLoaderBuilder apiDepLoader = new DependencyLoaderBuilder()
				.setExecutor(executor).setOutputDirectory(libsFolder.resolve("api"));
		DependencyLoaderBuilder internalDepLoader = new DependencyLoaderBuilder()
				.setExecutor(executor).setOutputDirectory(libsFolder.resolve("internal"));

		var internalDeps = addInternalDepsStart();
		addApiDeps(apiDepLoader);
		addInternalDepsFinish(internalDepLoader, internalDeps);

		launcher = new BootstrapLauncher("LibertyBans", getClass().getClassLoader(),
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
	
	private Dependency readDependency0(String simpleDependencyName) {
		URL url = getClass().getClassLoader().getResource("dependencies/" + simpleDependencyName);
		try (InputStream is = url.openStream()) {

			String[] value = new String(is.readAllBytes(), StandardCharsets.US_ASCII).split("\n");
			return Dependency.of(value[0], value[1], value[2], value[3]);

		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}
	
	private CompletableFuture<Dependency> readDependency(String simpleDependencyName) {
		return CompletableFuture.supplyAsync(() -> readDependency0(simpleDependencyName), executor);
	}
	
	/*
	 * Dependencies here are publicly exposed, but cannot be relocated, because
	 * they are part of the API.
	 * 
	 */
	private void addApiDeps(DependencyLoaderBuilder loader) {
		if (!classExists("space.arim.omnibus.Omnibus")) {
			loader.addPair(readDependency0("omnibus"), Repositories.ARIM_GPL3);
		}
		if (!classExists("space.arim.uuidvault.api.UUIDVault")) {
			loader.addPair(readDependency0("uuidvault"), Repositories.ARIM_LESSER_GPL3);
		}
	}
	
	/*
	 * Other dependencies, those internal, are added to the isolated ClassLoader used by
	 * the core implementation. They do not need to be relocated.
	 * 
	 */
	
	private Map<InternalDependency, CompletableFuture<Dependency>> addInternalDepsStart() {
		EnumMap<InternalDependency, CompletableFuture<Dependency>> internalDeps = new EnumMap<>(InternalDependency.class);
		for (InternalDependency internalDep : InternalDependency.values()) {
			internalDeps.put(internalDep, readDependency(internalDep.id));
		}
		return internalDeps;
	}
	
	private void addInternalDepsFinish(DependencyLoaderBuilder loader,
			Map<InternalDependency, CompletableFuture<Dependency>> internalDeps) {
		/*
		 * Since Paper, Waterfall, Sponge, and Velocity include slf4j, it may already be present
		 */
		if (!classExists("org.slf4j.Logger")) {
			CompletableFuture<Dependency> slf4jApi = readDependency("slf4j-api");
			CompletableFuture<Dependency> slf4jJdk14 = readDependency("slf4j-jdk14");
			loader.addPair(slf4jApi.join(), Repositories.CENTRAL_REPO);
			loader.addPair(slf4jJdk14.join(), Repositories.CENTRAL_REPO);
		}

		for (InternalDependency internalDep : InternalDependency.values()) {
			if (internalDep.clazz != null) {
				warnRelocation(internalDep.name, internalDep.clazz);
			}
			loader.addPair(internalDeps.get(internalDep).join(), internalDep.repo);
		}
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
