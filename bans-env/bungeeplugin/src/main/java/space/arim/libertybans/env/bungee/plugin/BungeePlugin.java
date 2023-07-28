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

package space.arim.libertybans.env.bungee.plugin;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.Instantiator;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;
import space.arim.libertybans.bootstrap.LibraryDetection;
import space.arim.libertybans.bootstrap.Platform;
import space.arim.libertybans.bootstrap.Platforms;
import space.arim.libertybans.bootstrap.ProtectedLibrary;
import space.arim.libertybans.bootstrap.logger.JulBootstrapLogger;

import java.nio.file.Path;
import java.util.logging.Level;

public final class BungeePlugin extends Plugin {

	private BaseFoundation base;

	@Override
	public synchronized void onEnable() {
		if (base != null) {
			throw new IllegalStateException("Plugin enabled twice?");
		}
		base = initialize();
	}

	@Override
	public synchronized void onDisable() {
		BaseFoundation base = this.base;
		this.base = null;
		if (base == null) {
			getLogger().warning("LibertyBans wasn't launched; check your log for a startup error");
			return;
		}
		base.shutdown();
	}

	static Platform detectPlatform(Plugin plugin) {
		ProxyServer server = plugin.getProxy();
		return Platforms.bungeecord()
				// On BungeeCord slf4j is an internal dependency
				.slf4jSupport(LibraryDetection.eitherOf(
						new LibraryDetection.Slf4jPluginLoggerMethod(plugin),
						new LibraryDetection.ByClassLoaderScan(ProtectedLibrary.SLF4J_API, Plugin.class.getClassLoader())
				))
				.build(server.getName() + " " + server.getVersion());
	}

	private BaseFoundation initialize() {
		Path folder = getDataFolder().toPath();
		TaskScheduler scheduler = getProxy().getScheduler();

		LibertyBansLauncher launcher = new LibertyBansLauncher.Builder()
				.folder(folder)
				.logger(new JulBootstrapLogger(getLogger()))
				.platform(detectPlatform(this))
				.executor((cmd) -> scheduler.runAsync(this, cmd))
				.culpritFinder(new BungeeCulpritFinder(getLogger()))
				.build();
		ClassLoader launchLoader = launcher.attemptLaunch().join();
		BaseFoundation base;
		try {
			base = new Instantiator("space.arim.libertybans.env.bungee.BungeeLauncher", launchLoader)
					.invoke(Plugin.class, this, folder);
		} catch (IllegalArgumentException | SecurityException | ReflectiveOperationException ex) {
			getLogger().log(Level.WARNING, "Failed to launch LibertyBans", ex);
			return null;
		}
		base.startup();
		return base;
	}

}
