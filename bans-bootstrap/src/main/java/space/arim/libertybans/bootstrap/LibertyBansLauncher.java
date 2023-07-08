/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

import space.arim.libertybans.bootstrap.depend.BootstrapLauncher;
import space.arim.libertybans.bootstrap.depend.DependencyLoaderBuilder;
import space.arim.libertybans.bootstrap.depend.ExistingDependency;
import space.arim.libertybans.bootstrap.logger.BootstrapLogger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class LibertyBansLauncher {

	private final Path folder;
	private final BootstrapLogger logger;
	private final Platform platform;
	private final Executor executor;
	private final CulpritFinder culpritFinder;
	private final DistributionMode distributionMode;
	private final ClassLoader parentLoader;

	private LibertyBansLauncher(Path folder, BootstrapLogger logger, Platform platform, Executor executor,
								CulpritFinder culpritFinder, DistributionMode distributionMode, ClassLoader parentLoader) {
		this.folder = Objects.requireNonNull(folder, "folder");
		this.logger = Objects.requireNonNull(logger, "logger");
		this.platform = Objects.requireNonNull(platform, "platform");
		this.executor = Objects.requireNonNull(executor, "executor");
		this.culpritFinder = Objects.requireNonNull(culpritFinder, "culpritFinder");
		this.distributionMode = Objects.requireNonNull(distributionMode, "distributionMode");
		this.parentLoader = Objects.requireNonNull(parentLoader, "parentLoader");
	}

	private void filterLibrariesAndWarnRelocation(Set<ProtectedLibrary> librariesRequiringProtection) {

		StringJoiner collectedDetails = new StringJoiner("\n");
		// Check all libraries; keep those which still require protection
		for (Iterator<ProtectedLibrary> iterator = librariesRequiringProtection.iterator(); iterator.hasNext(); ) {
			ProtectedLibrary library = iterator.next();
			Class<?> libClass;
			try {
				libClass = Class.forName(library.sampleClass());
			} catch (ClassNotFoundException ignored) {
				iterator.remove();
				continue;
			}
			if (platform.hasHiddenHikariCP() && library == ProtectedLibrary.HIKARICP) {
				continue;
			}
			String pluginName = culpritFinder.findCulprit(libClass)
					.map((name) -> "Plugin '" + name + '\'')
					.orElse("<Unknown plugin>");
			collectedDetails.add(
					pluginName + " | " + library.libraryName() + " | " + libClass.getName()
			);
		}
		if (collectedDetails.length() == 0) {
			return;
		}
		if (Boolean.getBoolean("libertybans.relocationbug.disablecheck")) {
			logger.info("Discovered unrelocated classes: \n" + collectedDetails);
			return;
		}
		String line = "*******************************************";
		logger.warn(line + '\n'
				+ "We have detected bugs on your server which threaten your server's stability.\n"
				+ "LibertyBans will continue to operate unaffected, but we strongly suggest you fix these bugs."
				+ "\n\n"
				+ "These bugs are (most likely) due to other plugins' mistakes. "
				+ "Each of the following plugins has shaded a library but did not relocate it. "
				+ "You should report each bug to the plugin author."
				+ "\n\n"
				+ "Plugin Name | Library Name | Class Detected\n"
				+ "----------------------------------------------\n"
				+ collectedDetails
				+ "\n\n"
				+ "Note for advanced users: Understanding the consequences, you can minimize this warning by setting "
				+ "the system property libertybans.relocationbug.disablecheck to 'true'"
				+ '\n' + line);
	}

	private Set<DependencyBundle> determineNeededDependencies(Set<ProtectedLibrary> librariesRequiringProtection) {
		Set<DependencyBundle> bundles = EnumSet.noneOf(DependencyBundle.class);
		if (platform.isCaffeineProvided()) {
			librariesRequiringProtection.remove(ProtectedLibrary.CAFFEINE);
		} else {
			bundles.add(DependencyBundle.CAFFEINE);
		}
		if (platform.isJakartaProvided()) {
			librariesRequiringProtection.remove(ProtectedLibrary.JAKARTA_INJECT);
		} else {
			bundles.add(DependencyBundle.JAKARTA);
		}
		if (platform.hasKyoriAdventureSupport()) {
			librariesRequiringProtection.remove(ProtectedLibrary.KYORI_ADVENTURE);
			librariesRequiringProtection.remove(ProtectedLibrary.KYORI_EXAMINATION);
		} else {
			bundles.add(DependencyBundle.KYORI);
		}
		if (platform.hasSlf4jSupport()) {
			librariesRequiringProtection.remove(ProtectedLibrary.SLF4J_API);
			librariesRequiringProtection.remove(ProtectedLibrary.SLF4J_SIMPLE);
		} else {
			bundles.add(DependencyBundle.SLF4J);
		}
		bundles.add(DependencyBundle.SELF_IMPLEMENTATION);
		return bundles;
	}

	private void migrateLegacyDirectory(Path oldPath, Path newPath) throws IOException {
		if (Files.exists(oldPath) && !Files.isDirectory(newPath)) {
			Files.move(oldPath, newPath);
			logger.info("Migrated legacy directory " + oldPath + " to " + newPath + ".");
		}
	}

	private void migrateLegacyDirectories(Path internalFolder) {
		Path newHyperSqlFolder = internalFolder.resolve("hypersql");
		Path newLibrariesFolder = internalFolder.resolve("libraries");
		try {
			Files.createDirectories(internalFolder);
			migrateLegacyDirectory(folder.resolve("hypersql"), newHyperSqlFolder);
			migrateLegacyDirectory(folder.resolve("libraries"), newLibrariesFolder);
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	public CompletableFuture<ClassLoader> attemptLaunch() {

		Path internalFolder = folder.resolve("internal");
		// Migrate legacy directories from LibertyBans 1.0.x
		migrateLegacyDirectories(internalFolder);

		// Start with all libraries, then narrow down as necessary
		Set<ProtectedLibrary> librariesRequiringProtection = EnumSet.allOf(ProtectedLibrary.class);

		Set<ExistingDependency> existingDependencies = new HashSet<>();
		DependencyLoaderBuilder loader = new DependencyLoaderBuilder()
				.executor(executor)
				.outputDirectory(internalFolder.resolve("libraries"));

		for (DependencyBundle bundle : determineNeededDependencies(librariesRequiringProtection)) {
			switch (distributionMode) {
			case TESTING -> {
				if (bundle == DependencyBundle.SELF_IMPLEMENTATION) {
					continue;
				}
				bundle.prepareToDownload(loader);
			}
			case DEPLOY_AND_DOWNLOAD -> bundle.prepareToDownload(loader);
			case JAR_OF_JARS -> existingDependencies.add(bundle.existingDependency());
			default -> throw new IllegalArgumentException("Unknown distributionMode " + distributionMode);
			}
		}
		filterLibrariesAndWarnRelocation(librariesRequiringProtection);
		// Begin to download dependencies
		BootstrapLauncher<AttachableClassLoader> launcher = new BootstrapLauncher<>(
				new AttachableClassLoader(
						"LibertyBans-ClassLoader",
						(librariesRequiringProtection.isEmpty()) ?
								parentLoader : new FilteringClassLoader(parentLoader, librariesRequiringProtection)
				),
				loader.build(),
				existingDependencies
		);
		CompletableFuture<AttachableClassLoader> futureClassLoader = launcher.load();
		// Detect addons and attachments in the meantime
		Set<Path> additionalLibraries;
		try {
			// Addons
			Path addonsFolder = folder.resolve("addons");
			Files.createDirectories(addonsFolder);
			try (Stream<Path> addonStream = Files.list(addonsFolder)) {
				Set<Path> addonJars = addonStream
						.filter((library) -> library.getFileName().toString().endsWith(".jar"))
						.collect(Collectors.toCollection(HashSet::new));
				logger.info(addonJars.isEmpty() ?
						"No addons detected" : "Detected " + addonJars.size() + " addon(s)"
				);
				additionalLibraries = addonJars;
			}
			// Attachments
			Path attachmentsFolder = internalFolder.resolve("attachments");
			Files.createDirectories(attachmentsFolder);
			try (Stream<Path> attachments = Files.list(attachmentsFolder)) {
				attachments.forEach(additionalLibraries::add);
			}
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return futureClassLoader.thenApply((classLoader) -> {
			// Attach additional libraries here
			additionalLibraries.forEach(classLoader::addJarPath);
			return classLoader;
		});
	}

	public static final class Builder {

		public Step0 folder(Path folder) {
			return new Step0(folder);
		}
	}

	public static final class Step0 {

		private final Path folder;

		private Step0(Path folder) {
			this.folder = folder;
		}

		public Step1 logger(BootstrapLogger logger) {
			return new Step1(folder, logger);
		}
	}
	public static final class Step1 {

		private final Path folder;
		private final BootstrapLogger logger;

		private Step1(Path folder, BootstrapLogger logger) {
			this.folder = folder;
			this.logger = logger;
		}

		public Step2 platform(Platform platform) {
			return new Step2(this, platform);
		}
	}

	public static final class Step2 {

		private final Step1 step1;
		private final Platform platform;

		private Step2(Step1 step1, Platform platform) {
			this.step1 = step1;
			this.platform = platform;
		}

		public FinalStep executor(Executor executor) {
			return new FinalStep(this, executor);
		}
	}

	public static final class FinalStep {

		private final Step2 step2;
		private final Executor executor;

		private CulpritFinder culpritFinder;
		private DistributionMode distributionMode;
		private ClassLoader parentLoader;

		private FinalStep(Step2 step2, Executor executor) {
			this.step2 = step2;
			this.executor = executor;
		}

		public FinalStep culpritFinder(CulpritFinder culpritFinder) {
			this.culpritFinder = culpritFinder;
			return this;
		}

		public FinalStep distributionMode(DistributionMode distributionMode) {
			this.distributionMode = distributionMode;
			return this;
		}

		public FinalStep parentLoader(ClassLoader parentLoader) {
			this.parentLoader = parentLoader;
			return this;
		}

		public LibertyBansLauncher build() {
			CulpritFinder culpritFinder = Objects.requireNonNullElse(this.culpritFinder, (clazz) -> Optional.empty());
			DistributionMode distributionMode = Objects.requireNonNullElseGet(this.distributionMode, () -> {
				if (getClass().getResource("/bans-executable_identifier") != null) {
					return DistributionMode.JAR_OF_JARS;
				}
				return DistributionMode.DEPLOY_AND_DOWNLOAD;
			});
			ClassLoader parentLoader = Objects.requireNonNullElse(this.parentLoader, getClass().getClassLoader());
			Step1 step1 = step2.step1;
			return new LibertyBansLauncher(
					step1.folder,
					step1.logger,
					step2.platform,
					executor,
					culpritFinder,
					distributionMode,
					parentLoader
			);
		}
	}

}
