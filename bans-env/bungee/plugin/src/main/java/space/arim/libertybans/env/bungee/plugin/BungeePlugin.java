/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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
import space.arim.libertybans.bootstrap.*;
import space.arim.libertybans.bootstrap.logger.JulBootstrapLogger;

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

	static Platform.Builder detectPlatform(Plugin plugin) {
		ProxyServer server = plugin.getProxy();
		return Platform
				.builder(Platform.Category.BUNGEECORD)
				.nameAndVersion(server.getName(), server.getVersion())
				// On BungeeCord slf4j is an internal dependency, on Waterfall it is API
				.slf4jSupport(LibraryDetection.eitherOf(
						new LibraryDetection.Slf4jPluginLoggerMethod(plugin),
						new LibraryDetection.ByClassLoaderScan(ProtectedLibrary.SLF4J_API, Plugin.class.getClassLoader())
				))
				.snakeYamlProvided(LibraryDetection.enabled());
	}

	private BaseFoundation initialize() {
		LibertyBansLauncher launcher = new LibertyBansLauncher.Builder()
				.folder(getDataFolder().toPath())
				.logger(new JulBootstrapLogger(getLogger()))
				.platform(detectPlatform(this))
				.executor((cmd) -> getProxy().getScheduler().runAsync(this, cmd))
				.culpritFinder(new BungeeCulpritFinder(getLogger()))
				.build();
		Payload<Plugin> payload = launcher.getPayload(this);
		ClassLoader launchLoader = launcher.attemptLaunch().join();
		BaseFoundation base;
		try {
			base = new Instantiator(
					"space.arim.libertybans.env.bungee.BungeeLauncher", launchLoader
			).invoke(payload);
		} catch (IllegalArgumentException | SecurityException | ReflectiveOperationException ex) {
			getLogger().log(Level.WARNING, "Failed to launch LibertyBans", ex);
			return null;
		}
		base.startup();
		return base;
	}

}
