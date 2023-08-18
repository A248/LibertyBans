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

import space.arim.libertybans.api.scope.ServerScope;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

public class ScopeParsing {

	public static final String GLOBAL_SCOPE_USER_INPUT = "*";

	private static final int SCOPE_VALUE_LENGTH_LIMIT = 32;

	static void checkScopeValue(String value) {
		if (value.isEmpty()) {
			throw new IllegalArgumentException("Scope is empty");
		}
		if (value.length() > SCOPE_VALUE_LENGTH_LIMIT) {
			throw new IllegalArgumentException("Scope length must be less than " + SCOPE_VALUE_LENGTH_LIMIT);
		}
	}

	public ServerScope specificScope(String server) {
		return new SpecificServerScope(server);
	}

	public ServerScope category(String category) {
		return new CategoryScope(category);
	}

	public ServerScope globalScope() {
		return GlobalScope.INSTANCE;
	}

	/**
	 * Same as {@link #parseFrom(String)} except yields optional.
	 *
	 * @param userInput the user input
	 * @return the scope if the input matched an appropriate pattern and was valid
	 */
	public Optional<ServerScope> parseInputOptionally(String userInput) {
		ServerScope scope;
		try {
			scope = parseFrom(userInput);
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
		return Optional.of(scope);
	}

	/**
	 * Inverse of display(scope, GLOBAL_SCOPE_USER_INPUT)
	 *
	 * @param userInput the user input
	 * @return the scope if the input matched an appropriate pattern and was valid
	 * @throws IllegalArgumentException if not a valid scope
	 */
	public ServerScope parseFrom(String userInput) {

		if (userInput.equals(GLOBAL_SCOPE_USER_INPUT)) {
			return globalScope();

		} else if (userInput.startsWith("server:")) {
			String server = userInput.substring("server:".length());
			return specificScope(server);

		} else if (userInput.startsWith("category:")) {
			String category = userInput.substring("category:".length());
			return category(category);
		}
		throw new IllegalArgumentException(
				"Scopes must be of the form '" + GLOBAL_SCOPE_USER_INPUT + "', 'server:' plus a server, " +
						"or 'category:' plus a category"
		);
	}

	String display(ServerScope scope, String defaultIfGlobal) {
		return deconstruct(scope, (type, value) -> {
			String prefix = switch (type) {
				case GLOBAL -> defaultIfGlobal;
				case SERVER -> "server:";
				case CATEGORY -> "category:";
			};
			return prefix + value;
		});
	}

	public <R> R deconstruct(ServerScope scope, BiFunction<ScopeType, String, R> computeResult) {
		ScopeType type;
		String value;
		if (scope instanceof GlobalScope) {
			type = ScopeType.GLOBAL;
			value = "";
		} else if (scope instanceof SpecificServerScope specificServer) {
			type = ScopeType.SERVER;
			value = specificServer.server();
		} else if (scope instanceof CategoryScope category) {
			type = ScopeType.CATEGORY;
			value = category.category();
		} else {
			Objects.requireNonNull(scope, "scope");
			throw new IllegalArgumentException(
					"Unknown server scope implementation: " + scope + " (" + scope.getClass() + ')'
			);
		}
		return computeResult.apply(type, value);
	}
}
