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
package space.arim.libertybans.it;

import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

class ClosablePath implements CloseableResource {

	private final Path tempDir;

	ClosablePath(Path tempDir) {
		this.tempDir = tempDir;
	}

	@Override
	public void close() throws IOException {
		try (Stream<Path> walk = Files.walk(tempDir)) {
			walk.sorted(Comparator.reverseOrder())
				.forEach((path) -> {
					try {
						Files.delete(path);
					} catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
				});
		}
	}

}
