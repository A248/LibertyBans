/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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
import java.nio.file.StandardCopyOption;
import java.util.Set;

/**
 * An object which acts on a dependency newly downloaded
 *
 */
public interface DownloadProcessor {

	/**
	 * Processes a downloaded dependency. <br>
	 * <br>
	 * Each path returned is treated as a jar which will be added to the {@code BootstrapLauncher}.
	 * The paths returned should be inside {@code targetDirectory}. <br>
	 * <br>
	 * The multiplicity of paths returned allows a {@code DownloadProcessor} implementation to
	 * extract further jars from the downloaded dependency (e.g., a jar containing further jars).
	 *
	 * @param dependency the downloaded dependency
	 * @param jarFilePath the path of the downloaded dependency
	 * @param targetDirectory the location where dependencies should be copied to
	 * @return the jar files to add to the launcher
	 * @throws UncheckedIOException if an I/O error occurs
	 */
	Set<Path> onDependencyDownload(Dependency dependency, Path jarFilePath, Path targetDirectory);

	/**
	 * Gets a download processor which simply adds the dependency
	 * to the launcher
	 *
	 * @return a simple processor
	 */
	static DownloadProcessor simple() {
		return (dependency, jarFilePath, targetDirectory) -> {
			if (jarFilePath.startsWith(targetDirectory)) {
				// Most usages of DependencyLoader end up here
				return Set.of(jarFilePath);
			}
			Path newJarFilePath = targetDirectory.resolve(jarFilePath.getFileName());
			try {
				Files.copy(jarFilePath, newJarFilePath, StandardCopyOption.COPY_ATTRIBUTES);
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
			return Set.of(newJarFilePath);
		};
	}

}
