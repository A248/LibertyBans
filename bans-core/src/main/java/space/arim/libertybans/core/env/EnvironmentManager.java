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

package space.arim.libertybans.core.env;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import space.arim.libertybans.bootstrap.StartupException;
import space.arim.libertybans.core.Part;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.scope.InternalScopeManager;

import java.util.List;
import java.util.Set;

@Singleton
public final class EnvironmentManager implements Part {

	private final Environment environment;
	private final Configs configs;
	private final EnvServerNameDetection serverNameDetection;
	private final InternalScopeManager scopeManager;

	private Set<PlatformListener> listeners;
	private PlatformListener[] commandAliases;

	@Inject
	public EnvironmentManager(Environment environment, Configs configs,
							  EnvServerNameDetection serverNameDetection, InternalScopeManager scopeManager) {
		this.environment = environment;
		this.configs = configs;
		this.serverNameDetection = serverNameDetection;
		this.scopeManager = scopeManager;
	}

	public Object platformAccess() {
		return environment.platformAccess();
	}

	private void registerListeners() {
		Set<PlatformListener> listeners = environment.createListeners();
		listeners.forEach(PlatformListener::register);
		this.listeners = listeners;
	}

	private void registerAliases() {
		List<String> aliases = configs.getMainConfig().commands().aliases();
		PlatformListener[] commands = new PlatformListener[aliases.size()];
		for (int n = 0; n < commands.length; n++) {
			commands[n] = environment.createAliasCommand(aliases.get(n));
			commands[n].register();
		}
		this.commandAliases = commands;
	}

	@Override
	public void startup() {
		registerListeners();
		registerAliases();
		serverNameDetection.detectName(scopeManager);
	}

	@Override
	public void restart() {
		throw new StartupException("Internal error, EnvironmentManager#restart should not be called");
	}

	@Override
	public void shutdown() {
		listeners.forEach(PlatformListener::unregister);
		listeners = null;
		for (PlatformListener commandAlias : commandAliases) {
			commandAlias.unregister();
		}
		scopeManager.clearDetectedServerName();
	}

}
