/* 
 * LibertyBans-api
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-api. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.api.scope;

/**
 * Factory which produces {@link ServerScope}s
 * 
 * @author A248
 *
 */
public interface ScopeManager {

	/**
	 * Gets a scope applying to a specific server. <br>
	 * <br>
	 * The server string must be non{@literal -}empty and less long than an
	 * implementation{@literal -}dependent length. The current limit is set at 32
	 * characters but this may change.
	 * 
	 * @param server the server
	 * @return a scope applying to the server
	 * @throws IllegalArgumentException if {@code server} is empty or too big
	 */
	ServerScope specificScope(String server);

	/**
	 * Gets the global scope, applying to all servers
	 * 
	 * @return the global scope
	 */
	ServerScope globalScope();

	/**
	 * Gets a scope applying to the current server
	 * 
	 * @return a scope applying to the current server
	 */
	ServerScope currentServerScope();

}
