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

package space.arim.libertybans.bootstrap;

public final class Platforms {

	private Platforms() {}

	public static boolean detectGetSlf4jLoggerMethod(Object plugin) {
		try {
			plugin.getClass().getMethod("getSLF4JLogger");
			return true;
		} catch (NoSuchMethodException ignored) {
			return false;
		}
	}

	public static Platform velocity() {
		return Platform.forCategory(Platform.Category.VELOCITY)
				.slf4jSupport(true).kyoriAdventureSupport(true).build("Velocity");
	}

}
