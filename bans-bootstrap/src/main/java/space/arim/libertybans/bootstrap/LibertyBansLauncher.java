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
import space.arim.libertybans.bootstrap.depend.DependencyLoaderBuilder;
import space.arim.libertybans.bootstrap.depend.ExistingDependency;
import space.arim.libertybans.bootstrap.logger.BootstrapLogger;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class LibertyBansLauncher {

	private final BootstrapLogger logger;
	private final Platform platform;
	private ClassLoader parentClassLoader = getClass().getClassLoader();
	private final Path libsFolder;
	private final Executor executor;
	private final Path jarFile;
	private final CulpritFinder culpritFinder;
	
	public LibertyBansLauncher(BootstrapLogger logger, Platform platform, Path folder, Executor executor,
							   Path jarFile, CulpritFinder culpritFinder) {
		this.logger = Objects.requireNonNull(logger, "logger");
		this.platform = Objects.requireNonNull(platform, "platform");
		libsFolder = folder.resolve("libraries");
		this.executor = Objects.requireNonNull(executor, "executor");
		this.jarFile = Objects.requireNonNull(jarFile, "jarFile");
		this.culpritFinder = Objects.requireNonNull(culpritFinder, "culpritFinder");
	}
	
	public LibertyBansLauncher(BootstrapLogger logger, Platform platform, Path folder, Executor executor,
							   Path jarFile) {
		this(logger, platform, folder, executor, jarFile, (clazz) -> "");
	}

	public void overrideParentClassLoader(ClassLoader parentClassLoader) {
		this.parentClassLoader = parentClassLoader;
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
		String line = "*******************************************";
		logger.warn(line + '\n'
				+ pluginName + " has shaded the library '"
				+ libName + "' but did not relocate it. This may or may not pose problems. "
				+ "Contact the author of this plugin and tell them to relocate their dependencies. "
				+ "Unrelocated class detected was " + libClass.getName()
				+ '\n' + line);
	}
	
	private Set<DependencyBundle> determineNeededDependencies() {
		Set<DependencyBundle> bundles = EnumSet.noneOf(DependencyBundle.class);
		if (!platform.hasSlf4jSupport()) {
			warnRelocation("Slf4j", "org.slf4j.Logger");
			warnRelocation("Slf4j-Simple", "org.slf4j.simple.SimpleLogger");
			bundles.add(DependencyBundle.SLF4J);
		}
		if (!platform.isCaffeineProvided()) {
			warnRelocation("Caffeine", "com.github.benmanes.caffeine.cache.Caffeine");
			bundles.add(DependencyBundle.CAFFEINE);
		}
		if (!platform.hasKyoriAdventureSupport()) {
			warnRelocation("Kyori-Adventure", "net.kyori.adventure.audience.Audience");
			warnRelocation("Kyori-Examination", "net.kyori.examination.Examinable");
			bundles.add(DependencyBundle.KYORI);
		}
		bundles.add(DependencyBundle.SELF_IMPLEMENTATION);
		return bundles;
	}

	public DistributionMode distributionMode() {
		if (getClass().getResource("/bans-executable_identifier") != null) {
			return DistributionMode.JAR_OF_JARS;
		}
		return DistributionMode.DEPLOY_AND_DOWNLOAD;
	}

	public CompletableFuture<ClassLoader> attemptLaunch() {
		for (RelocationCheck checkForUnrelocation : RelocationCheck.values()) {
			var classPresence = checkForUnrelocation.classPresence();
			warnRelocation(classPresence.dependencyName(), classPresence.className());
		}
		DependencyLoaderBuilder loader = new DependencyLoaderBuilder()
				.executor(executor)
				.outputDirectory(libsFolder);
		Set<ExistingDependency> existingDependencies = new HashSet<>();
		DistributionMode distributionMode = distributionMode();
		for (DependencyBundle bundle : determineNeededDependencies()) {
			switch (distributionMode) {
			case TESTING:
				if (bundle == DependencyBundle.SELF_IMPLEMENTATION) {
					continue;
				}
				bundle.prepareToDownload(loader);
				break;
			case DEPLOY_AND_DOWNLOAD:
				bundle.prepareToDownload(loader);
				break;
			case JAR_OF_JARS:
				existingDependencies.add(new ExistingDependency(jarFile, bundle.existingFileProcessor()));
				break;
			default:
				throw new IllegalArgumentException("Unknown distributionMode " + distributionMode);
			}
		}
		BootstrapLauncher launcher = BootstrapLauncher.create(
				"LibertyBans", parentClassLoader,
				loader.build(), existingDependencies);
		return launcher.load();
	}
	
}
