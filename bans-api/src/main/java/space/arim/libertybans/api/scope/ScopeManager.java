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

import java.util.Optional;
import java.util.Set;

/**
 * Factory which produces {@link ServerScope}s
 *
 */
public interface ScopeManager {

	/**
	 * Gets a scope applying to a specific server. <br>
	 * <br>
	 * The server string must not be empty and must be less long than a certain length dependent
	 * on the implementation. The current limit is set at 32 characters but this may change.
	 * 
	 * @param server the server
	 * @return a scope applying to the server
	 * @throws IllegalArgumentException if {@code server} is empty or too big
	 */
	ServerScope specificScope(String server);

	/**
	 * Gets a scope applying to a user defined category. <br>
	 * <br>
	 * The category string must not be empty and must be less long than a certain length dependent
	 * on the implementation. The current limit is set at 32 characters but this may change.
	 *
	 * @param category the category
	 * @return a scope applying to the category
	 * @throws IllegalArgumentException if {@code category} is empty or too big
	 */
	ServerScope category(String category);

	/**
	 * Gets the global scope, applying to all servers and categories
	 * 
	 * @return the global scope
	 */
	ServerScope globalScope();

	/**
	 * Gets a scope applying to the current server. This will yield a scope which, when used to punish players,
	 * will enforce their punishments on the current server only. <br>
	 * <br>
	 * This is usually not the appropriate scope with which to <b>select</b> punishments (because only punishments
	 * made specifically for the server would be selected). Instead, see {@link #scopesApplicableToCurrentServer()}. <br>
	 * <br>
	 * The "current server" is defined with respect to an instance of the API implementation. Thus, using this method
	 * will apply to the proxy, backend server, or other application running the current instance. <br>
	 * <br>
	 * Please note that, in some circumstances, the current server scope will not be available, depending on user
	 * configuration. Use {@link #currentServerScopeOrFallback()} if you want to fallback to the scope specified
	 * in the user's configuration for this purpose.
	 * 
	 * @return if available, the scope applying to the current server only. On a proxy instance, using this scope will
	 * affect the entire proxy. On a backend server it will affect the backend server.
	 */
	Optional<ServerScope> currentServerScope();

	/**
	 * Gets the current server scope if it is available (see {@link #currentServerScope()}. Otherwise,
	 * uses the server scope which the user has configured as a fallback for the current server scope
	 * when it is unavailable.
	 *
	 * @return the scope applying to the current server only, or the fallback if not available
	 */
	ServerScope currentServerScopeOrFallback();

	/**
	 * Gets all scopes applying to the current server as defined by user configuration. This will necessarily
	 * include both the global scope ({@link #globalScope()}) and the current server scope
	 * ({@link #currentServerScope()}. <br>
	 * <br>
	 * The "current server" is defined with respect to an instance of the API implementation. Thus, using this method
	 * will yield scopes applying to the proxy, backend server, or other application running this instance.
	 *
	 * @return all scopes applicable to the current server
	 */
	Set<ServerScope> scopesApplicableToCurrentServer();

	/**
	 * The scope configured as the default with which to punish. For example, if an operator punishes a victim without
	 * specifying a scope explicitly, this scope is used.
	 *
	 * @return the default punishing scope
	 */
	ServerScope defaultPunishingScope();

}
