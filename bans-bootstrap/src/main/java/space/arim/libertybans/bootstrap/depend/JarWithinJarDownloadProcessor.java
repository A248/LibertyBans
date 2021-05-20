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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * A download processor which extracts jars located inside of a mega jar. <br>
 * <br>
 * Can optionally overwrite existing jars during extraction.
 *
 */
public final class JarWithinJarDownloadProcessor implements DownloadProcessor {

	private final String jarDirectory;
	private final boolean replaceExisting;

	private JarWithinJarDownloadProcessor(String jarDirectory, boolean replaceExisting) {
		this.jarDirectory = Objects.requireNonNull(jarDirectory, "jarDirectory");
		this.replaceExisting = replaceExisting;
	}

	/**
	 * Creates from the directory where the nested jars are located. <br>
	 * <br>
	 * Will <b>not</b> replace existing jars when unpacking the mega jar.
	 *
	 * @param jarDirectory the directory of nested jars
	 */
	public JarWithinJarDownloadProcessor(String jarDirectory) {
		this(jarDirectory, false);
	}

	/**
	 * Sets whether to replace existing jars. False by default
	 *
	 * @param replaceExisting true to replace existing jars
	 * @return a download processor which replaces existing jars
	 */
	public JarWithinJarDownloadProcessor replaceExisting(boolean replaceExisting) {
		return new JarWithinJarDownloadProcessor(jarDirectory, replaceExisting);
	}

	@Override
	public Set<Path> onDependencyDownload(Dependency dependency, Path jarFilePath, Path targetDirectory) {
		Set<Path> outputPaths = new HashSet<>();
		try (JarFile jar = new JarFile(jarFilePath.toFile())) {
			jar.stream().filter((entry) -> {
				return entry.getName().startsWith(jarDirectory);
			}).forEach((entry) -> {
				String targetJarName = entry.getName().substring(jarDirectory.length());
				if (targetJarName.isBlank() || targetJarName.equals("/")) {
					return;
				}
				if (targetJarName.startsWith("/")) {
					targetJarName = targetJarName.substring(1);
				}
				Path destination = targetDirectory.resolve(targetJarName);
				outputPaths.add(destination);
				if (replaceExisting || !Files.exists(destination)) {
					try (InputStream inputStream = jar.getInputStream(entry)) {
						Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
				}
			});
		} catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return outputPaths;
	}
}
