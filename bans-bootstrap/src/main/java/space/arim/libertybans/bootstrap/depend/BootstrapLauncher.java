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
package space.arim.libertybans.bootstrap.depend;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
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

	private BootstrapLauncher(AddableURLClassLoader classLoader, DependencyLoader dependencyLoader) {
		this.classLoader = classLoader;
		this.dependencyLoader = dependencyLoader;
	}

	public static BootstrapLauncher create(String programName, ClassLoader parentClassLoader,
			DependencyLoader dependencyLoader) {
		return new BootstrapLauncher(
				new AddableURLClassLoader(programName, parentClassLoader),
				dependencyLoader);
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}
	
	private CompletableFuture<Set<Path>> loadJarPaths(DependencyLoader loader) {
		return loader.execute().thenApply((results) -> {

			Set<Path> jarPaths = new HashSet<>(results.size());
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
				jarPaths.add(entry.getValue().getJarFile());
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
			/*
			 * Cleanup previously downloaded but now-unused dependencies (old versions)
			 */
			try (Stream<Path> fileStream = Files.list(loader.getOutputDirectory())) {
				fileStream.filter((file) -> !jarPaths.contains(file)).forEach((toDelete) -> {
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

	public CompletableFuture<Void> load() {
		return loadJarPaths(dependencyLoader).thenAccept((paths) -> {
			try {
				for (Path path : paths) {
					classLoader.addURL(path.toUri().toURL());
				}
			} catch (MalformedURLException ex) {
				throw new BootstrapException("Unable to convert Path to URL", ex);
			}
		});
	}

}
