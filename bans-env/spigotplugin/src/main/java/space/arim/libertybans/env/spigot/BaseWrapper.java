/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.env.spigot;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.CulpritFinder;
import space.arim.libertybans.bootstrap.Instantiator;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;
import space.arim.libertybans.bootstrap.Platform;
import space.arim.libertybans.bootstrap.Platforms;
import space.arim.libertybans.bootstrap.logger.BootstrapLogger;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class BaseWrapper {

	private final BaseFoundation base;
	
	BaseWrapper(BaseFoundation base) {
		this.base = base;
	}
	
	static class Creator {
		
		private final JavaPlugin plugin;
		private final BootstrapLogger logger;
		private final Path jarFile;
		
		Creator(JavaPlugin plugin, BootstrapLogger logger, Path jarFile) {
			this.plugin = plugin;
			this.logger = logger;
			this.jarFile = jarFile;
		}

		private boolean detectAdventure() {
			try {
				Class<?> audienceClass = Class.forName("net.kyori.adventure.audience.Audience");
				return audienceClass.isAssignableFrom(Player.class);
			} catch (ClassNotFoundException ex) {
				return false;
			}
		}

		Platform detectPlatform() {
			boolean slf4j = Platforms.detectGetSlf4jLoggerMethod(plugin)
					|| Platforms.detectLibrary("org.slf4j.Logger", JavaPlugin.class.getClassLoader());
			return Platform.forCategory(Platform.Category.BUKKIT)
					.slf4jSupport(slf4j)
					.kyoriAdventureSupport(detectAdventure())
					.build(plugin.getServer().getVersion());
		}

		BaseWrapper create() {
			Platform platform = detectPlatform();
			Path folder = plugin.getDataFolder().toPath();
			ExecutorService executor = Executors.newCachedThreadPool();
			ClassLoader launchLoader;
			try {
				// Using CulpritFinder#decorate prevents java.lang.UnsupportedClassVersionError during linkage
				CulpritFinder culpritFinder = CulpritFinder.decorate(new SpigotCulpritFinder());
				LibertyBansLauncher launcher = new LibertyBansLauncher(logger,
						platform, folder, executor, jarFile, culpritFinder);
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
