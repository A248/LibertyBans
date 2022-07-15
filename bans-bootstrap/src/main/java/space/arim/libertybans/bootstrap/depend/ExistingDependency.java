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
import java.nio.file.Path;
import java.util.Set;

/**
 * An existing dependency
 *
 */
public interface ExistingDependency {

	/**
	 * Processes this file dependency. <br>
	 * <br>
	 * Each path returned is treated as a jar which will be added to the {@code BootstrapLauncher}.
	 * The paths returned should be inside {@code targetDirectory}. <br>
	 * <br>
	 * The multiplicity of paths returned allows a {@code ExistingDependency} implementation to
	 * extract as many jars as necessary.
	 *
	 * @param targetDirectory the location where dependencies should be copied to
	 * @return the jar files to add to the launcher
	 * @throws IOException if an I/O error occurs
	 * @throws UncheckedIOException if an I/O error occurs
	 */
	Set<Path> onDependencyDownload(Path targetDirectory) throws IOException;

}
