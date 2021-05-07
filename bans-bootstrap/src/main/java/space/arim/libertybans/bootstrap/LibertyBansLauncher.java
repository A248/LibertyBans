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

import space.arim.libertybans.bootstrap.depend.BootstrapLauncher;
import space.arim.libertybans.bootstrap.depend.DependencyLoader;
import space.arim.libertybans.bootstrap.depend.DependencyLoaderBuilder;
import space.arim.libertybans.bootstrap.depend.LocatableDependency;
import space.arim.libertybans.bootstrap.logger.BootstrapLogger;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class LibertyBansLauncher {

	private final BootstrapLogger logger;
	private final Platform platform;
	private final Path libsFolder;
	private final Executor executor;
	private final CulpritFinder culpritFinder;
	
	public LibertyBansLauncher(BootstrapLogger logger, Platform platform, Path folder, Executor executor,
							   CulpritFinder culpritFinder) {
		this.logger = logger;
		this.platform = platform;
		libsFolder = folder.resolve("libraries");
		this.executor = executor;
		this.culpritFinder = culpritFinder;
	}
	
	public LibertyBansLauncher(BootstrapLogger logger, Platform platform, Path folder, Executor executor) {
		this(logger, platform, folder, executor, (clazz) -> "");
	}
	
	private static Class<?> classForName(String clazzName) {
		try {
			return Class.forName(clazzName);
		} catch (ClassNotFoundException ignored) {
			return null;
		}
	}
	
	private String findCulpritWhoFailedToRelocate(Class<?> libClass) {
		String pluginName = culpritFinder.findCulprit(libClass);
		if (pluginName == null || pluginName.isEmpty()) {
			return "An unknown plugin";
		}
		return "Plugin '" + pluginName + '\'';
	}
	
	private void warnRelocation(String libName, String clazzName) {
		Class<?> libClass = classForName(clazzName);
		if (libClass == null) {
			return;
		}
		String pluginName = findCulpritWhoFailedToRelocate(libClass);
		logger.warn(pluginName + " has shaded the library '"
				+ libName + "' but did not relocate it. This may or may not pose problems. "
				+ "Contact the author of this plugin and tell them to relocate their dependencies. "
				+ "Unrelocated class detected was " + libClass.getName());
	}
	
	private DependencyLoader createDependencyLoader() {
		DependencyLoaderBuilder loader = new DependencyLoaderBuilder()
				.executor(executor)
				.outputDirectory(libsFolder);
		Set<CompletableFuture<LocatableDependency>> jarsToDownload = new HashSet<>();
		if (!platform.hasSlf4jSupport()) {
			warnRelocation("Slf4j", "org.slf4j.Logger");
			jarsToDownload.add(locate(InternalDependency.SLF4J_API));
			jarsToDownload.add(locate(InternalDependency.SLF4J_JUL));
		}
		boolean downloadSelfDependencies = !skipSelfDependencies();
		if (!platform.hasKyoriAdventureSupport()) {
			warnRelocation("Kyori-Adventure", "net.kyori.adventure.audience.Audience");
			warnRelocation("Kyori-Examination", "net.kyori.examination.Examinable");
			if (downloadSelfDependencies) {
				jarsToDownload.add(locate(InternalDependency.KYORI_BUNDLE));
			}
		}
		if (downloadSelfDependencies) {
			jarsToDownload.add(locate(InternalDependency.SELF_IMPLEMENTATION));
		}
		for (InternalDependency checkForUnrelocation : InternalDependency.values()) {
			checkForUnrelocation.classPresence().ifPresent((classPresence) -> {
				warnRelocation(classPresence.dependencyName(), classPresence.className());
			});
		}
		for (CompletableFuture<LocatableDependency> download : jarsToDownload) {
			LocatableDependency locatedDependency = download.join();
			loader.addDependencyPair(locatedDependency.dependency(), locatedDependency.repository());
		}
		return loader.build();
	}

	private CompletableFuture<LocatableDependency> locate(InternalDependency dependency) {
		return dependency.locateUsing(executor);
	}

	boolean skipSelfDependencies() {
		return getClass().getResource("/abort_self-dependency_download") != null;
	}

	public CompletableFuture<ClassLoader> attemptLaunch() {
		BootstrapLauncher launcher = BootstrapLauncher.create(
				"LibertyBans", getClass().getClassLoader(),
				createDependencyLoader());
		return launcher.load().thenApply((ignore) -> launcher.getClassLoader());
	}
	
}
