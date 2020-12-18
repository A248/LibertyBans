/* 
 * LibertyBans-env-bungeeplugin
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-bungeeplugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-bungeeplugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-bungeeplugin. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.bungee;

import java.nio.file.Path;
import java.util.concurrent.Executor;

import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.CulpritFinder;
import space.arim.libertybans.bootstrap.DependencyPlatform;
import space.arim.libertybans.bootstrap.Instantiator;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;
import space.arim.libertybans.bootstrap.logger.BootstrapLogger;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.scheduler.TaskScheduler;

class BaseWrapper {

	private final BaseFoundation base;
	
	BaseWrapper(BaseFoundation base) {
		this.base = base;
	}
	
	static class Creator {
		
		private final Plugin plugin;
		private final BootstrapLogger logger;
		
		Creator(Plugin plugin, BootstrapLogger logger) {
			this.plugin = plugin;
			this.logger = logger;
		}
		
		BaseWrapper create() {
			DependencyPlatform platform = DependencyPlatform.detectGetSlf4jLoggerMethod(plugin) ?
					DependencyPlatform.WATERFALL : DependencyPlatform.BUNGEE;
			Path folder = plugin.getDataFolder().toPath();
			TaskScheduler scheduler = plugin.getProxy().getScheduler();
			Executor executor = (cmd) -> scheduler.runAsync(plugin, cmd);

			// Using CulpritFinder#decorate prevents java.lang.UnsupportedClassVersionError during linkage
			CulpritFinder culpritFinder = CulpritFinder.decorate((clazz) -> {
				PluginDescription description = new PluginClassLoaderReflection(clazz).getProvidingPlugin();
				if (description != null) {
					return description.getName() + " " + description.getVersion();
				}
				return null;
			});
			LibertyBansLauncher launcher = new LibertyBansLauncher(logger, platform, folder, executor, culpritFinder);
			ClassLoader launchLoader = launcher.attemptLaunch().join();

			if (launchLoader == null) {
				logger.warn("Failed to launch LibertyBans");
				return null;
			}
			BaseFoundation base;
			try {
				base = new Instantiator("space.arim.libertybans.env.bungee.BungeeLauncher", launchLoader)
						.invoke(Plugin.class, plugin, folder);
			} catch (IllegalArgumentException | SecurityException | ReflectiveOperationException ex) {
				logger.warn("Failed to launch LibertyBans", ex);
				return null;
			}
			base.startup();
			return new BaseWrapper(base);
		}
		
	}
	
	void close() {
		base.shutdown();
	}
	
}
