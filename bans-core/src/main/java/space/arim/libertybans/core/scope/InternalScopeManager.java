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

public interface InternalScopeManager extends ScopeManager {

	String getServer(ServerScope scope, String defaultIfGlobal);
	
	/**
	 * Checks that a server scope is nonnull and of the right implementation class
	 * 
	 * @param scope the server scope
	 * @throws NullPointerException if {@code scope} is null
	 * @throws IllegalArgumentException if {@code scope} is a foreign implementation
	 */
	void checkScope(ServerScope scope);
	
}
