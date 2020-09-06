/* 
 * LibertyBans-env-velocityplugin
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-velocityplugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-velocityplugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-velocityplugin. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.velocity;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executor;

import org.slf4j.Logger;

import com.google.inject.Inject;

import space.arim.libertybans.bootstrap.BaseEnvironment;
import space.arim.libertybans.bootstrap.Instantiator;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;
import space.arim.libertybans.bootstrap.plugin.PluginInfo;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.Scheduler;

@Plugin(id = PluginInfo.ANNOTE_ID, name = PluginInfo.NAME, version = PluginInfo.VERSION, authors = {
		"A248" }, description = PluginInfo.DESCRIPTION, url = PluginInfo.URL)
public class VelocityPlugin {

	final ProxyServer server;
	final Path folder;
	private final Logger logger;
	
	private BaseEnvironment base;
	
	@Inject
	public VelocityPlugin(ProxyServer server, @DataDirectory Path folder, Logger logger) {
		this.server = server;
		this.folder = folder;
		this.logger = logger;
	}
	
	@Subscribe
	public synchronized void onProxyInitialize(@SuppressWarnings("unused") ProxyInitializeEvent evt) {
		if (base != null) {
			throw new IllegalStateException("Proxy initialised twice?");
		}
		PluginContainer plugin = server.getPluginManager().fromInstance(this).get();
		Scheduler scheduler = server.getScheduler();
		Executor executor = (cmd) -> scheduler.buildTask(this, cmd).schedule();

		LibertyBansLauncher launcher = new LibertyBansLauncherVelocity(this, executor);
		ClassLoader launchLoader = launcher.attemptLaunch().join();

		if (launchLoader == null) {
			logger.warn("Failed to launch LibertyBans");
			return;
		}
		BaseEnvironment base;
		try {
			base = new Instantiator("space.arim.libertybans.env.velocity.VelocityEnv", launchLoader)
					.invoke(Map.Entry.class, Map.entry(plugin, server), Path.class, folder);
		} catch (IllegalArgumentException | SecurityException | ReflectiveOperationException ex) {
			logger.warn("Failed to launch LibertyBans", ex);
			return;
		}
		base.startup();
		this.base = base;
	}
	
	@Subscribe
	public synchronized void onProxyShutdown(@SuppressWarnings("unused") ProxyShutdownEvent evt) {
		BaseEnvironment base = this.base;
		if (base == null) {
			logger.warn("LibertyBans never launched; nothing to shutdown");
			return;
		}
		base.shutdown();
	}
	
}
