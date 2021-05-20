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

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

/**
 * A dependency which already exists on the filesystem
 *
 */
public final class ExistingDependency {

	private final Path jarPath;
	private final DownloadProcessor downloadProcessor;

	public ExistingDependency(Path jarPath, DownloadProcessor downloadProcessor) {
		this.jarPath = Objects.requireNonNull(jarPath, "jarPath");
		this.downloadProcessor = Objects.requireNonNull(downloadProcessor, "downloadProcessor");
	}

	/**
	 * Gets the jar path for this dependency itself
	 *
	 * @return the path for the dependency itself
	 */
	public Path jarPath() {
		return jarPath;
	}

	/**
	 * "Downloads" this existing dependency by adding it to the launcher. <br>
	 * <br>
	 * Very similar to {@link DownloadProcessor#onDependencyDownload(Dependency, Path, Path)}
	 *
	 * @param targetDirectory the target directory
	 * @return the jar files to add to the launcher
	 * @throws UncheckedIOException if an I/O error occurs
	 */
	public Set<Path> onDependencyDownload(Path targetDirectory) {
		return downloadProcessor.onDependencyDownload(
				new Dependency("existing", "existing", "1", new byte[0], downloadProcessor),
				jarPath,
				targetDirectory);
	}

	@Override
	public String toString() {
		return "ExistingDependency{" +
				"jarPath=" + jarPath +
				", downloadProcessor=" + downloadProcessor +
				'}';
	}
}
