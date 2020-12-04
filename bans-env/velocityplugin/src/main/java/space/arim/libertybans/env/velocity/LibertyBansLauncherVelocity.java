/* 
 * LibertyBans-env-velocity
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-velocity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-velocity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-velocity. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.velocity;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Executor;

import space.arim.libertybans.bootstrap.DependencyPlatform;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;
import space.arim.libertybans.bootstrap.logger.Slf4jBootstrapLogger;

import com.velocitypowered.api.plugin.PluginManager;

class LibertyBansLauncherVelocity extends LibertyBansLauncher {

	private final VelocityPlugin plugin;
	
	LibertyBansLauncherVelocity(VelocityPlugin plugin, Executor executor) {
		super(new Slf4jBootstrapLogger(plugin.logger), DependencyPlatform.VELOCITY, plugin.folder, executor);
		this.plugin = plugin;
	}

	@Override
	protected void addUrlsToExternalClassLoader(ClassLoader apiClassLoader, Set<Path> paths) {
		PluginManager pluginManager = plugin.server.getPluginManager();
		for (Path path : paths) {
			// Explicitly use plugin instance instead of PluginContainer
			// Avoids https://github.com/VelocityPowered/Velocity/pull/387
			pluginManager.addToClasspath(plugin, path);
		}
	}

}
