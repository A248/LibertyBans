/* 
 * LibertyBans-env-spigotplugin
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-spigotplugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-spigotplugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-spigotplugin. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.spigot;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.CulpritFinder;
import space.arim.libertybans.bootstrap.DependencyPlatform;
import space.arim.libertybans.bootstrap.Instantiator;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;
import space.arim.libertybans.bootstrap.logger.BootstrapLogger;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

class BaseWrapper {

	private final BaseFoundation base;
	
	BaseWrapper(BaseFoundation base) {
		this.base = base;
	}
	
	static class Creator {
		
		private final JavaPlugin plugin;
		private final BootstrapLogger logger;
		
		Creator(JavaPlugin plugin, BootstrapLogger logger) {
			this.plugin = plugin;
			this.logger = logger;
		}
		
		BaseWrapper create() {
			DependencyPlatform platform = DependencyPlatform.detectGetSlf4jLoggerMethod(plugin) ?
					DependencyPlatform.PAPER : DependencyPlatform.SPIGOT;
			Path folder = plugin.getDataFolder().toPath();
			ExecutorService executor = Executors.newCachedThreadPool();
			ClassLoader launchLoader;
			try {
				// Using CulpritFinder#decorate prevents java.lang.UnsupportedClassVersionError during linkage
				CulpritFinder culpritFinder = CulpritFinder.decorate((clazz) -> {
					JavaPlugin plugin;
					try {
						 plugin = JavaPlugin.getProvidingPlugin(clazz);
					} catch (IllegalArgumentException ignored) {
						return null;
					}
					PluginDescriptionFile description = plugin.getDescription();
					return description.getName() + " " + description.getVersion();
				});
				LibertyBansLauncher launcher = new LibertyBansLauncher(logger,
						platform, folder, executor, culpritFinder);
				launchLoader = launcher.attemptLaunch().join();
			} finally {
				executor.shutdown();
				assert executor.isTerminated();
			}
			if (launchLoader == null) {
				logger.warn("Failed to launch LibertyBans");
				return null;
			}
			BaseFoundation base;
			try {
				base = new Instantiator("space.arim.libertybans.env.spigot.SpigotLauncher", launchLoader)
						.invoke(JavaPlugin.class, plugin, folder);
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
