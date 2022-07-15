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
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public final class ExtractNestedJars implements ExistingDependency {

	private final URL jarResource;
	private final String intermediateFileName;

	public ExtractNestedJars(URL jarResource, String intermediateFileName) {
		this.jarResource = Objects.requireNonNull(jarResource, "jarResource");
		this.intermediateFileName = Objects.requireNonNull(intermediateFileName, "intermediateFileName");
	}

	@Override
	public Set<Path> onDependencyDownload(Path targetDirectory) throws IOException {
		Set<Path> outputPaths = new HashSet<>();
		// First, determine if we need to extract any jars. Snapshots must always be updated
		Map<String, Path> jarsToExtract = new HashMap<>();

		try (InputStream resourceInput = jarResource.openStream();
			 JarInputStream jarResourceInput = new JarInputStream(resourceInput)) {

			JarEntry entry;
			while ((entry = jarResourceInput.getNextJarEntry()) != null) {
				String entryName = entry.getName();
				if (!entryName.endsWith(".jar")) {
					continue;
				}
				Path extractTo = targetDirectory.resolve(entryName);
				if (entryName.endsWith("-SNAPSHOT.jar") || Files.notExists(extractTo)) {
					jarsToExtract.put(entryName, extractTo);
				}
				outputPaths.add(extractTo);
			}
		}
		// Second, extract those jars which require it, creating an intermediate jar file if necessary
		if (!jarsToExtract.isEmpty()) {
			Path intermediateFile = targetDirectory.resolve(intermediateFileName);
			try (InputStream resourceInput = jarResource.openStream()) {
				Files.copy(resourceInput, intermediateFile, StandardCopyOption.REPLACE_EXISTING);
			}
			try (JarFile intermediatejarFile = new JarFile(intermediateFile.toFile())) {
				for (Map.Entry<String, Path> jarToExtract : jarsToExtract.entrySet()) {
					extractNestedJar(
							intermediatejarFile,
							jarToExtract.getKey(),
							jarToExtract.getValue()
					);
				}
			}
		}
		return outputPaths;
	}

	private void extractNestedJar(JarFile sourceJarFile, String entryName, Path destination) throws IOException {
		JarEntry entry = sourceJarFile.getJarEntry(entryName);
		try (InputStream nestedJarStream = sourceJarFile.getInputStream(entry)) {
			Files.copy(nestedJarStream, destination, StandardCopyOption.REPLACE_EXISTING);
		}
	}

}
