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
import java.util.concurrent.Executor;

import space.arim.libertybans.bootstrap.DependencyPlatform;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;

public class LibertyBansLauncherVelocity extends LibertyBansLauncher {

	private final VelocityPlugin plugin;
	
	public LibertyBansLauncherVelocity(VelocityPlugin plugin, Executor executor) {
		super(DependencyPlatform.VELOCITY, plugin.folder, executor, (c) -> "");
		this.plugin = plugin;
	}
	
	@Override
	protected boolean addUrlsToExternalClassLoader(ClassLoader apiClassLoader, Path[] paths) {
		PluginManager pm = plugin.server.getPluginManager();
		PluginContainer plugin = pm.fromInstance(this.plugin).get();
		for (Path path : paths) {
			pm.addToClasspath(plugin, path);
		}
		return true;
	}

}
