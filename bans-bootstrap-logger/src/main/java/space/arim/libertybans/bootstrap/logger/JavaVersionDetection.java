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
package space.arim.libertybans.bootstrap.logger;

import java.util.List;

public class JavaVersionDetection {

	private final BootstrapLogger logger;

	public JavaVersionDetection(BootstrapLogger logger) {
		this.logger = logger;
	}

	public boolean detectVersion() {
		try {
			List.of();
		} catch (NoSuchMethodError java8) {
			return wrongVersion(8);
		}
		try {
			List.copyOf(List.of());
		} catch (NoSuchMethodError java9) {
			return wrongVersion(9);
		}
		try {
			"".isBlank();
		} catch (NoSuchMethodError java10) {
			return wrongVersion(10);
		}
		return true;
	}

	private boolean wrongVersion(int version) {
		logger.error(
				"Your version of Java (Detected Java " + version + ") is outdated. Please upgrade to at least JDK 11 "
				+ "in order for LibertyBans to work properly. To install "
				+ "Java 11 or any later version, see https://adoptopenjdk.net/");
		return false;
	}

}
