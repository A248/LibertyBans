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
package space.arim.libertybans.bootstrap;

public enum DependencyPlatform {

	SPIGOT(Category.BUKKIT),
	PAPER(Category.BUKKIT),
	BUNGEE(Category.BUNGEE),
	WATERFALL(Category.BUNGEE),
	VELOCITY(Category.VELOCITY);
	
	private final Category category;
	
	private DependencyPlatform(Category category) {
		this.category = category;
	}
	
	public boolean hasSlf4jSupport() {
		switch (this) {
		case SPIGOT:
		case BUNGEE:
			return false;
		default:
			return true;
		}
	}
	
	public static boolean detectGetSlf4jLoggerMethod(Object plugin) {
		try {
			plugin.getClass().getMethod("getSLF4JLogger");
			return true;
		} catch (NoSuchMethodException ignored) {
			return false;
		}
	}
	
	public Category getCategory() {
		return category;
	}
	
	public enum Category {
		BUKKIT,
		BUNGEE,
		VELOCITY
	}
	
}
