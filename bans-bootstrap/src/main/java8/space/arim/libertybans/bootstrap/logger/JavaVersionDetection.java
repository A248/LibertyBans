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
package space.arim.libertybans.bootstrap.logger;

import java.util.Collection;
import java.util.List;

public class JavaVersionDetection {

	private final BootstrapLogger logger;

	public JavaVersionDetection(BootstrapLogger logger) {
		this.logger = logger;
	}

	public boolean detectVersion() {
		try {
			List.class.getDeclaredMethod("of");
		} catch (NoSuchMethodException java8) {
			return wrongVersion(8);
		}
		try {
			List.class.getDeclaredMethod("copyOf", Collection.class);
		} catch (NoSuchMethodException java9) {
			return wrongVersion(9);
		}
		try {
			String.class.getDeclaredMethod("isBlank");
		} catch (NoSuchMethodException java10) {
			return wrongVersion(10);
		}
		return true;
	}

	private boolean wrongVersion(int version) {
		logger.error(
				"*****************************************************************\n" +
				"*****************************************************************\n" +
				"*********************** ATTENTION *******************************\n" +
				"Your version of Java is outdated.\n" +
				"Your Java version: " + version + "\n" +
				"Required Java version: 11 or newer\n" +
				"\n" +
				"Please upgrade to at least Java 11 in order for LibertyBans to work.\n" +
				"To install Java 11 or any later version, contact your host or download it from https://adoptopenjdk.net/\n" +
				"*****************************************************************\n" +
				"*****************************************************************\n" +
				"*****************************************************************");
		return false;
	}

}
