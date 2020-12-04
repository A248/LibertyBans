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

import java.util.function.Function;

@FunctionalInterface
public interface CulpritFinder {

	/**
	 * Gets the name of the plugin which failed to shade the specified library class
	 * 
	 * @param libraryClass the class which should have been relocated
	 * @return the name of the plugin, or a null or empty string if unknown
	 */
	String findCulprit(Class<?> libraryClass);
	
	/**
	 * Decorates a {@link Function} whose signature matches a culprit finder
	 * 
	 * @param function the function
	 * @return a culprit finder using the function
	 */
	static CulpritFinder decorate(Function<Class<?>, String> function) {
		return function::apply;
	}
	
}
