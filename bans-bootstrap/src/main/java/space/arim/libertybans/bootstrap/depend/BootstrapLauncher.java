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

package space.arim.libertybans.bootstrap.depend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Composition of a {@link DependencyLoader} used to add dependencies to
 * an AddableURLClassLoader.
 * 
 * @author A248
 *
 */
public final class BootstrapLauncher {

	private final AddableURLClassLoader classLoader;
	private final DependencyLoader dependencyLoader;
	private final Set<ExistingDependency> existingDependencies;

	private BootstrapLauncher(AddableURLClassLoader classLoader, DependencyLoader dependencyLoader,
							  Set<ExistingDependency> existingDependencies) {
		this.classLoader = classLoader;
		this.dependencyLoader = Objects.requireNonNull(dependencyLoader, "dependencyLoader");
		this.existingDependencies = Set.copyOf(existingDependencies);
	}

	public static BootstrapLauncher create(String programName, ClassLoader parentClassLoader,
			DependencyLoader dependencyLoader, Set<ExistingDependency> existingDependencies) {
		return new BootstrapLauncher(
				new AddableURLClassLoader(programName, parentClassLoader),
				dependencyLoader, existingDependencies);
	}
	
	private CompletableFuture<Set<Path>> loadJarPaths(DependencyLoader loader) {
		Path targetDirectory = loader.getOutputDirectory();
		return loader.execute().thenApply((results) -> {

			// JARs which will be used on the classpath/modulepath
			Set<Path> jarPaths = new HashSet<>(results.size());
			// JARs present which should not be deleted. Includes jarPaths
			Set<Path> dontDelete = new HashSet<>(results.size());

			Set<BootstrapException> failures = new HashSet<>();
			for (Map.Entry<Dependency, DownloadResult> entry : results.entrySet()) {

				Dependency dependency = entry.getKey();
				DownloadResult result = entry.getValue();
				switch (result.getResultType()) {
				case HASH_MISMATCH:
					failures.add(new BootstrapException(
							"Failed to download dependency: " + dependency + " . Reason: Hash mismatch, " + "expected "
									+ Dependency.bytesToHex(result.getExpectedHash()) + " but got "
									+ Dependency.bytesToHex(result.getActualHash())));
					continue;
				case ERROR:
					failures.add(new BootstrapException(
							"Failed to download dependency: " + dependency + " . Reason: Exception",
							result.getException()));
					continue;
				default:
					break;
				}
				Path jarFile = result.getJarFile();
				// Keep downloaded JARs so they don't need to be re-downloaded
				dontDelete.add(jarFile);
				jarPaths.addAll(
						dependency.downloadProcessor().onDependencyDownload(dependency, jarFile, targetDirectory));
			}
			if (!failures.isEmpty()) {
				if (failures.size() == 1) {
					throw failures.iterator().next();
				}
				BootstrapException ex = new BootstrapException("Failed to download dependencies. View and report details.");
				for (BootstrapException failure : failures) {
					ex.addSuppressed(failure);
				}
				throw ex;
			}
			for (ExistingDependency existingDependency : existingDependencies) {
				// Keep existing external dependencies
				dontDelete.add(existingDependency.jarPath());
				jarPaths.addAll(existingDependency.onDependencyDownload(targetDirectory));
			}
			// Don't delete immediately-needed JARs
			dontDelete.addAll(jarPaths);
			/*
			 * Cleanup previous but now-unused jars
			 */
			try (Stream<Path> fileStream = Files.list(targetDirectory)) {
				fileStream.filter((file) -> !dontDelete.contains(file)).forEach((toDelete) -> {
					try {
						Files.delete(toDelete);
					} catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
				});
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
			return jarPaths;
		});
	}

	public CompletableFuture<AddableURLClassLoader> load() {
		return loadJarPaths(dependencyLoader).thenApply((paths) -> {
			try {
				for (Path path : paths) {
					classLoader.addURL(path.toUri().toURL());
				}
			} catch (MalformedURLException ex) {
				throw new BootstrapException("Unable to convert Path to URL", ex);
			}
			return classLoader;
		});
	}

}
