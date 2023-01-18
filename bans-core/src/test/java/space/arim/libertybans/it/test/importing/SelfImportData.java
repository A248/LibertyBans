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

package space.arim.libertybans.it.test.importing;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class SelfImportData {

	private final Path temporaryFolder;

	public SelfImportData(Path temporaryFolder) {
		this.temporaryFolder = temporaryFolder;
	}

	private Path copy(String sample) throws IOException {
		Path sampleFolder = temporaryFolder.resolve(sample);
		Path databaseFolder = sampleFolder.resolve("internal").resolve("hypersql");
		Files.createDirectories(databaseFolder);

		for (String extensionToCopy : new String[] {".script", ".properties"}) {
			String filename = "punishments-database" + extensionToCopy;
			URL resource = getClass().getResource("/import-data/self/" + sample + "/" + filename);
			assert resource != null : "Missing resource";
			try (InputStream inputStream = resource.openStream()) {
				Files.copy(inputStream, databaseFolder.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		return sampleFolder;
	}

	public Path copyBlueTree242() throws IOException {
		return copy("bluetree242");
	}

}
