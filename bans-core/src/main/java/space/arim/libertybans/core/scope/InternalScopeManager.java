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

package space.arim.libertybans.core.scope;

import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.scope.ServerScope;

import java.util.Optional;
import java.util.function.BiFunction;

public interface InternalScopeManager extends ScopeManager {

	ServerScope deserialize(ScopeType scopeType, String value);

	<R> R deconstruct(ServerScope scope, BiFunction<ScopeType, String, R> computeResult);

	String display(ServerScope scope, String defaultIfGlobal);

	Optional<ServerScope> parseFrom(String userInput);

	/**
	 * Checks that a server scope is nonnull and of the right implementation class
	 * 
	 * @param scope the server scope
	 * @throws NullPointerException if {@code scope} is null
	 * @throws IllegalArgumentException if {@code scope} is a foreign implementation
	 * @return the same scope
	 */
	ServerScope checkScope(ServerScope scope);

	/**
	 * Whether to automatically detect the name of this server instance
	 *
	 * @return whether to detect the server name
	 */
	boolean serverNameUndetected();

	/**
	 * Sets the automatically detected name of this server instance
	 *
	 * @param serverName the server name
	 */
	void detectServerName(String serverName);

	/**
	 * Clears the automatically detected server name
	 *
	 */
	void clearDetectedServerName();

}
