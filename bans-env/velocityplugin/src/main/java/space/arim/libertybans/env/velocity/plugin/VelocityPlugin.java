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

package space.arim.libertybans.env.velocity.plugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.ProxyVersion;
import org.slf4j.Logger;
import space.arim.libertybans.bootstrap.*;
import space.arim.libertybans.bootstrap.plugin.PluginInfo;

import java.nio.file.Path;

@Plugin(id = PluginInfo.ID,
		name = PluginInfo.NAME,
		version = PluginInfo.VERSION,
		authors = { PluginInfo.AUTHOR },
		description = PluginInfo.DESCRIPTION,
		url = PluginInfo.URL,
		dependencies = {
			@Dependency(id = "luckperms", optional = true)
		})
public final class VelocityPlugin {

	private final PluginContainer plugin;
	private final ProxyServer server;
	private final Path folder;
	private final Logger logger;

	private BaseFoundation base;

	@Inject
	public VelocityPlugin(PluginContainer plugin, ProxyServer server, @DataDirectory Path folder, Logger logger) {
		this.plugin = plugin;
		this.server = server;
		this.folder = folder;
		this.logger = logger;
	}

	@Subscribe
	public synchronized void onProxyInitialize(ProxyInitializeEvent evt) {
		if (base != null) {
			throw new IllegalStateException("Proxy initialised twice?");
		}
		base = initialize();
	}

	@Subscribe
	public synchronized void onProxyShutdown(ProxyShutdownEvent evt) {
		BaseFoundation base = this.base;
		this.base = null;
		if (base == null) {
			logger.warn("LibertyBans wasn't launched; check your log for a startup error");
			return;
		}
		base.shutdown();
	}

	private BaseFoundation initialize() {

		ProxyVersion velocityVersion = server.getVersion();
		ClassLoader platformClassLoader = PluginContainer.class.getClassLoader();

		LibertyBansLauncher launcher = new LibertyBansLauncher.Builder()
				.folder(folder)
				.logger(new Slf4jBootstrapLogger(logger))
				.platform(Platform
						.builder(Platform.Category.VELOCITY)
						.nameAndVersion(velocityVersion.getName(), velocityVersion.getVersion())
						.slf4jSupport(LibraryDetection.enabled())
						.kyoriAdventureSupport(LibraryDetection.enabled())
						.snakeYamlProvided(LibraryDetection.enabled())
						// Caffeine is an internal dependency; possibly Jakarta too
						.caffeineProvided(new LibraryDetection.ByClassLoaderScan(ProtectedLibrary.CAFFEINE, platformClassLoader))
						.jakartaProvided(new LibraryDetection.ByClassLoaderScan(ProtectedLibrary.JAKARTA_INJECT, platformClassLoader)))
				.executor((cmd) -> server.getScheduler().buildTask(plugin, cmd).schedule())
				.culpritFinder(new VelocityCulpritFinder(server))
				.build();
		Payload<PluginContainer> payload = launcher.getPayload(plugin);
		ClassLoader launchLoader = launcher.attemptLaunch().join();
		BaseFoundation base;
		try {
			base = new Instantiator(
					"space.arim.libertybans.env.velocity.VelocityLauncher", launchLoader
			).invoke(payload, ProxyServer.class, server);
		} catch (IllegalArgumentException | SecurityException | ReflectiveOperationException ex) {
			logger.warn("Failed to launch LibertyBans", ex);
			return null;
		}
		base.startup();
		return base;
	}

}
