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

import space.arim.libertybans.bootstrap.depend.AddableURLClassLoader;
import space.arim.libertybans.bootstrap.depend.BootstrapLauncher;
import space.arim.libertybans.bootstrap.depend.DependencyLoaderBuilder;
import space.arim.libertybans.bootstrap.depend.ExistingDependency;
import space.arim.libertybans.bootstrap.logger.BootstrapLogger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class LibertyBansLauncher {

	private final BootstrapLogger logger;
	private final Platform platform;
	private final Path folder;
	private final Executor executor;
	private final Path jarFile;
	private final CulpritFinder culpritFinder;

	public LibertyBansLauncher(BootstrapLogger logger, Platform platform, Path folder, Executor executor,
							   Path jarFile, CulpritFinder culpritFinder) {
		this.logger = Objects.requireNonNull(logger, "logger");
		this.platform = Objects.requireNonNull(platform, "platform");
		this.folder = Objects.requireNonNull(folder, "folder");
		this.executor = Objects.requireNonNull(executor, "executor");
		this.jarFile = Objects.requireNonNull(jarFile, "jarFile");
		this.culpritFinder = Objects.requireNonNull(culpritFinder, "culpritFinder");
	}

	public LibertyBansLauncher(BootstrapLogger logger, Platform platform, Path folder, Executor executor,
							   Path jarFile) {
		this(logger, platform, folder, executor, jarFile, (clazz) -> "");
	}

	private String findCulpritWhoFailedToRelocate(Class<?> libClass) {
		String pluginName = culpritFinder.findCulprit(libClass);
		if (pluginName == null || pluginName.isEmpty()) {
			return "<Unknown plugin>";
		}
		return "Plugin '" + pluginName + '\'';
	}

	private void warnRelocation(String libName, String clazzName) {
		Class<?> libClass;
		try {
			libClass = Class.forName(clazzName);
		} catch (ClassNotFoundException ignored) {
			return;
		}
		String pluginName = findCulpritWhoFailedToRelocate(libClass);
		String line = "*******************************************";
		String message = line + '\n'
				+ pluginName + " has a critical bug. That plugin has shaded the library '"
				+ libName + "' but did not relocate it, which will pose problems. "
				+ "\n\n"
				+ "LibertyBans is not guaranteed to function if you do not fix this bug in " + pluginName
				+ "\n\n"
				+ "Contact the author of this plugin and tell them to relocate their dependencies. "
				+ "Unrelocated class detected was " + libClass.getName()
				+ "\n\n"
				+ "Note for advanced users: Understanding the consequences, you can disable this check by setting "
				+ "the system property libertybans.relocationbug.disablecheck to 'true'"
				+ '\n' + line;
		if (!Boolean.getBoolean("libertybans.relocationbug.disablecheck") && distributionMode() == DistributionMode.JAR_OF_JARS) {
			// In development builds we have no patience for bugs
			throw new IllegalStateException(message);
		}
		// In release builds, attempt to run despite the bug
		logger.warn(message);
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

	public final CompletableFuture<ClassLoader> attemptLaunch() {
		return attemptLaunch(getClass().getClassLoader());
	}

	public final CompletableFuture<ClassLoader> attemptLaunch(ClassLoader parentClassLoader) {
		for (RelocationCheck library : RelocationCheck.values()) {
			warnRelocation(library.libName(), library.className());
		}
		DependencyLoaderBuilder loader = new DependencyLoaderBuilder()
				.executor(executor)
				.outputDirectory(folder.resolve("libraries"));
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
		// Begin to download dependencies
		BootstrapLauncher launcher = BootstrapLauncher.create(
				"LibertyBans", parentClassLoader,
				loader.build(), existingDependencies);
		CompletableFuture<AddableURLClassLoader> futureClassLoader = launcher.load();
		// Detect addons in the meantime
		Set<Path> addons;
		try {
			Path addonsFolder = folder.resolve("addons");
			Files.createDirectories(addonsFolder);
			addons = Files.list(addonsFolder)
					.filter((library) -> library.getFileName().toString().endsWith(".jar"))
					.collect(Collectors.toUnmodifiableSet());
			if (!addons.isEmpty()) {
				logger.info("Detected " + addons.size() + " addon(s)");
			}
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return futureClassLoader.thenApply((classLoader) -> {
			// Attach addons here
			for (Path addon : addons) {
				try {
					classLoader.addURL(addon.toUri().toURL());
				} catch (MalformedURLException ex) {
					throw new UncheckedIOException(ex);
				}
			}
			return classLoader;
		});
	}
	
}
