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
import java.io.UncheckedIOException;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import space.arim.libertybans.bootstrap.depend.BootstrapException;
import space.arim.libertybans.bootstrap.depend.BootstrapLauncher;
import space.arim.libertybans.bootstrap.depend.Dependency;
import space.arim.libertybans.bootstrap.depend.DependencyLoaderBuilder;
import space.arim.libertybans.bootstrap.logger.BootstrapLogger;

public class LibertyBansLauncher {

	private final BootstrapLogger logger;
	private final DependencyPlatform platform;
	private final Path libsFolder;
	private final Executor executor;
	private final CulpritFinder culpritFinder;
	
	public LibertyBansLauncher(BootstrapLogger logger, DependencyPlatform platform, Path folder, Executor executor,
			CulpritFinder culpritFinder) {
		this.logger = logger;
		this.platform = platform;
		libsFolder = folder.resolve("libs");
		this.executor = executor;
		this.culpritFinder = culpritFinder;
	}
	
	public LibertyBansLauncher(BootstrapLogger logger, DependencyPlatform platform, Path folder, Executor executor) {
		this(logger, platform, folder, executor, (clazz) -> "");
	}
	
	protected void addUrlsToExternalClassLoader(ClassLoader apiClassLoader, Set<Path> paths) {
		if (!(apiClassLoader instanceof URLClassLoader)) {
			throw new IllegalArgumentException(
					"To use the default LibertyBansLauncher, the plugin must be loaded through a URLClassLoader");
		}
		logger.info(
				"You may receive a warning about illegal reflective access to URLClassLoader#addURL. "
				+ "This is harmless but unavoidable. See https://github.com/A248/LibertyBans/wiki/URLClassLoader%23addURL-warning");
		Method addUrlMethod;
		try {
			addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
			addUrlMethod.setAccessible(true);
		} catch (NoSuchMethodException | SecurityException | InaccessibleObjectException ex) {
			throw new BootstrapException("Failed to attach dependencies to API ClassLoader (locate method)", ex);
		}
		try {
			for (Path path : paths) {
				URL url = path.toUri().toURL();
				addUrlMethod.invoke(apiClassLoader, url);
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | MalformedURLException ex) {
			throw new BootstrapException("Failed to attach dependencies to API ClassLoader (invoke method)", ex);
		}
	}
	
	private static Class<?> classForName(String clazzName) {
		try {
			Class<?> result = Class.forName(clazzName);
			return result;
		} catch (ClassNotFoundException ignored) {
			return null;
		}
	}
	
	private static boolean classExists(String clazzName) {
		return classForName(clazzName) != null;
	}
	
	private String findCulpritWhoFailedToRelocate(Class<?> libClass) {
		String pluginName = culpritFinder.findCulprit(libClass);
		if (pluginName == null || pluginName.isEmpty()) {
			return "Unknown";
		}
		return pluginName;
	}
	
	private void warnRelocation(String libName, String clazzName) {
		Class<?> libClass = classForName(clazzName);
		if (libClass == null) {
			return;
		}
		String pluginName = findCulpritWhoFailedToRelocate(libClass);
		logger.warn("Plugin '" + pluginName + "' has shaded the library '"
				+ libName + "' but did not relocate it. This may or may not pose any problems. "
				+ "Contact the author of this plugin and tell them to relocate their dependencies. "
				+ "Unrelocated class detected was " + libClass.getName());
	}

	private Dependency readDependency0(String simpleDependencyName) {
		URL url = getClass().getClassLoader().getResource("dependencies/" + simpleDependencyName);
		try (InputStream inputStream = url.openStream()) {

			String fullString = new String(inputStream.readAllBytes(), StandardCharsets.US_ASCII);
			String[] lines = fullString.lines().toArray(String[]::new);
			if (lines.length < 4) {
				throw new IllegalArgumentException("Dependency file for " + simpleDependencyName + " is malformatted");
			}
			return Dependency.of(lines[0], lines[1], lines[2], lines[3]);

		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
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
		CompletableFuture<Dependency> selfApi = readDependency("self-api");
		if (!classExists("space.arim.omnibus.Omnibus")) {
			loader.addDependencyPair(readDependency0("omnibus"), Repositories.ARIM_LESSER_GPL3);
		}
		if (!skipSelfDependencies()) {
			loader.addDependencyPair(selfApi.join(), Repositories.ARIM_AFFERO_GPL3);
		}
	}
	
	/*
	 * Other dependencies, those internal, are added to the isolated ClassLoader used by
	 * the core implementation. They do not need to be relocated.
	 * 
	 */
	
	private Map<InternalDependency, CompletableFuture<Dependency>> addInternalDepsStart() {
		Map<InternalDependency, CompletableFuture<Dependency>> internalDeps = new EnumMap<>(InternalDependency.class);
		for (InternalDependency internalDep : InternalDependency.values()) {
			internalDeps.put(internalDep, readDependency(internalDep.id));
		}
		return internalDeps;
	}
	
	private void addInternalDepsFinish(DependencyLoaderBuilder loader,
			Map<InternalDependency, CompletableFuture<Dependency>> internalDeps) {

		if (!platform.hasSlf4jSupport()) {
			warnRelocation("Slf4j", "org.slf4j.Logger");
			CompletableFuture<Dependency> slf4jApi = readDependency("slf4j-api");
			CompletableFuture<Dependency> slf4jJdk14 = readDependency("slf4j-jdk14");
			loader.addDependencyPair(slf4jApi.join(), Repositories.CENTRAL_REPO);
			loader.addDependencyPair(slf4jJdk14.join(), Repositories.CENTRAL_REPO);
		}
		for (InternalDependency internalDep : InternalDependency.values()) {
			if (internalDep.clazz != null) {
				warnRelocation(internalDep.name, internalDep.clazz);
			}
			if (skipSelfDependencies() && internalDep == InternalDependency.SELF_CORE) {
				continue;
			}
			loader.addDependencyPair(internalDeps.get(internalDep).join(), internalDep.repo);
		}
	}
	
	/* Used in testing */
	protected boolean skipSelfDependencies() {
		return false;
	}
	
	private DependencyLoaderBuilder loaderBuilder(String subFolder) {
		return new DependencyLoaderBuilder().executor(executor).outputDirectory(libsFolder.resolve(subFolder));
	}

	public CompletableFuture<ClassLoader> attemptLaunch() {
		DependencyLoaderBuilder apiDepLoader = loaderBuilder("api");
		DependencyLoaderBuilder internalDepLoader = loaderBuilder("internal");

		Map<InternalDependency, CompletableFuture<Dependency>> internalDeps = addInternalDepsStart();
		addApiDeps(apiDepLoader);
		addInternalDepsFinish(internalDepLoader, internalDeps);

		BootstrapLauncher launcher = new BootstrapLauncher("LibertyBans", getClass().getClassLoader(),
				apiDepLoader.build(), internalDepLoader.build()) {

					@Override
					public void addUrlsToExternal(ClassLoader apiClassLoader, Set<Path> paths) {
						LibertyBansLauncher.this.addUrlsToExternalClassLoader(apiClassLoader, paths);
					}
		};
		return launcher.loadAll().thenApply((ignore) -> launcher.getInternalClassLoader());
	}
	
}
