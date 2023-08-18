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
package space.arim.libertybans.api.scope;

/**
 * The server-specific scope of a punishment. To get a scope, see
 * {@link ScopeManager}
 * 
 * @author A248
 *
 */
public interface ServerScope {

	/**
	 * Whether this scope applies to a server
	 * 
	 * @param server the server name
	 * @return true if applicable, false otherwise
	 * @deprecated For categorical scopes produced from {@link ScopeManager#category(String)}, it may not be known
	 * whether a scope applies to a certain server, because the configuration for the other server is not accessible
	 * by the current API instance.
	 */
	@Deprecated
	boolean appliesTo(String server);

	/**
	 * Determines equality with a given object. The other object must be a scope
	 * applying to the same servers as described in {@link #appliesTo(String)}
	 * 
	 * @param object the object to determine equality with
	 * @return true if equal, false otherwise
	 */
	@Override
	boolean equals(Object object);

}
