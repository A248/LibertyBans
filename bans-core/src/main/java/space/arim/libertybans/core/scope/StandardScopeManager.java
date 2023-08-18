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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.ScopeConfig;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

@Singleton
public class StandardScopeManager implements InternalScopeManager {

	private final Configs configs;
	private final ScopeParsing scopeParsing;

	private volatile String serverName;

	@Inject
	public StandardScopeManager(Configs configs, ScopeParsing scopeParsing) {
		this.configs = configs;
		this.scopeParsing = scopeParsing;
	}

	private ScopeConfig config() {
		return configs.getScopeConfig();
	}

	@Override
	public ServerScope specificScope(String server) {
		return new SpecificServerScope(server);
	}

	@Override
	public ServerScope category(String category) {
		return new CategoryScope(category);
	}

	@Override
	public ServerScope globalScope() {
		return GlobalScope.INSTANCE;
	}
	
	@Override
	public Optional<ServerScope> currentServerScope() {
		var nameConfig = config().serverName();
		if (nameConfig.autoDetect()) {
			return Optional.ofNullable(this.serverName).map(this::specificScope);
		} else {
			return Optional.of(specificScope(nameConfig.overrideValue()));
		}
	}

	@Override
	public ServerScope currentServerScopeOrFallback() {
		return currentServerScope().orElseGet(() -> {
			ConfiguredScope fallback = config().serverName().fallbackIfAutoDetectFails();
			// Don't call fallback.actualize(this) to prevent infinite recursion
			return fallback.rawOptional().orElse(globalScope());
		});
	}

	@Override
	public Set<ServerScope> scopesApplicableToCurrentServer() {
		Set<ServerScope> applicable = new HashSet<>();
		applicable.add(globalScope());
		applicable.add(currentServerScopeOrFallback());
		for (String category : config().categoriesApplicableToThisServer()) {
			applicable.add(category(category));
		}
		return applicable;
	}

	@Override
	public ServerScope defaultPunishingScope() {
		return switch (config().defaultPunishingScope()) {
			case GLOBAL -> globalScope();
			case THIS_SERVER -> currentServerScopeOrFallback();
			case PRIMARY_CATEGORY -> {
				var categories = config().categoriesApplicableToThisServer();
				if (categories.isEmpty()) {
					LoggerFactory.getLogger(getClass()).warn(
							"In the scope.yml configuration, the option default-punishing-scope is set to " +
									"'PRIMARY_CATEGORY', but no categories were specified. Either configure a category " +
									"or use a different default-punishing-scope value. The value 'THIS_SERVER' will be " +
									"temporarily used while you fix the configuration."
					);
					yield currentServerScopeOrFallback();
				} else {
					yield category(categories.get(0));
				}
			}
		};
	}

	@Override
	public ServerScope deserialize(ScopeType scopeType, String value) {
		return switch (scopeType) {
			case GLOBAL -> GlobalScope.INSTANCE;
			case SERVER -> new SpecificServerScope(value);
			case CATEGORY -> new CategoryScope(value);
		};
	}

	@Override
	public <R> R deconstruct(ServerScope scope, BiFunction<ScopeType, String, R> computeResult) {
		return scopeParsing.deconstruct(scope, computeResult);
	}

	@Override
	public String display(ServerScope scope, String defaultIfGlobal) {
		return scopeParsing.display(scope, defaultIfGlobal);
	}

	@Override
	public Optional<ServerScope> parseFrom(String userInput) {
		return scopeParsing.parseInputOptionally(userInput);
	}

	@Override
	public ServerScope checkScope(ServerScope scope) {
		// If deconstruct succeeds, scope is okay
		deconstruct(scope, (_type, _value) -> null);
		return scope;
	}

	@Override
	public boolean serverNameUndetected() {
		return serverName == null;
	}

	@Override
	public void detectServerName(String serverName) {
		Objects.requireNonNull(serverName);
		this.serverName = serverName;
	}

	@Override
	public void clearDetectedServerName() {
		serverName = null;
	}

}
