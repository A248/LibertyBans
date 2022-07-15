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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Executes a dependency loader and attaches its resulting jars
 *
 */
public final class BootstrapLauncher<J extends JarAttachment> {

	private final J jarAttachment;
	private final DependencyLoader dependencyLoader;
	private final Set<ExistingDependency> existingDependencies;

	public BootstrapLauncher(J jarAttachment, DependencyLoader dependencyLoader,
							 Set<ExistingDependency> existingDependencies) {
		this.jarAttachment = Objects.requireNonNull(jarAttachment, "jarAttachment");
		this.dependencyLoader = Objects.requireNonNull(dependencyLoader, "dependencyLoader");
		this.existingDependencies = Set.copyOf(existingDependencies);
	}

	private Set<Path> loadJarPaths(Map<Dependency, DownloadResult> results) {
		// JARs which will be used on the classpath/modulepath
		Set<Path> jarPaths = new HashSet<>(results.size(), 1f);

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
			jarPaths.add(result.getJarFile());
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
		try {
			Path targetDirectory = dependencyLoader.getOutputDirectory();
			// Add existing dependencies
			for (ExistingDependency existingDependency : existingDependencies) {
				jarPaths.addAll(existingDependency.onDependencyDownload(targetDirectory));
			}
			// Cleanup unused jars
			try (Stream<Path> fileStream = Files.list(targetDirectory)) {
				fileStream.filter((file) -> !jarPaths.contains(file)).forEach((toDelete) -> {
					try {
						Files.delete(toDelete);
					} catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
				});
			}
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return jarPaths;
	}

	public CompletableFuture<J> load() {
		return dependencyLoader.execute()
				.thenApply(this::loadJarPaths)
				.thenApply((jarPaths) -> {
					jarPaths.forEach(jarAttachment::addJarPath);
					return jarAttachment;
				});
	}

}
