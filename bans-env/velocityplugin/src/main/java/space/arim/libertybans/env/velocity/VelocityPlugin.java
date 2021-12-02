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

import javax.inject.Inject;

import org.slf4j.Logger;

import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.plugin.PluginInfo;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

@Plugin(id = PluginInfo.ANNOTE_ID, name = PluginInfo.NAME, version = PluginInfo.VERSION, authors = {
		"A248" }, description = PluginInfo.DESCRIPTION, url = PluginInfo.URL)
public class VelocityPlugin {

	final ProxyServer server;
	final Path folder;
	final Logger logger;

	private BaseFoundation base;

	@Inject
	public VelocityPlugin(ProxyServer server, @DataDirectory Path folder, Logger logger) {
		this.server = server;
		this.folder = folder;
		this.logger = logger;
	}

	@Subscribe
	public synchronized void onProxyInitialize(ProxyInitializeEvent evt) {
		if (base != null) {
			throw new IllegalStateException("Proxy initialised twice?");
		}
		base = new VelocityInitializer(this).initialize();
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

}
