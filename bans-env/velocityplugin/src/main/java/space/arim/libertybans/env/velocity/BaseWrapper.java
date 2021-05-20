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

import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.Instantiator;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.Scheduler;
import space.arim.libertybans.bootstrap.Platform;
import space.arim.libertybans.bootstrap.Platforms;
import space.arim.libertybans.bootstrap.logger.Slf4jBootstrapLogger;

class BaseWrapper {

	private final BaseFoundation base;
	
	BaseWrapper(BaseFoundation base) {
		this.base = base;
	}
	
	static class Creator {

		private final VelocityPlugin velocityPlugin;
		
		Creator(VelocityPlugin velocityPlugin) {
			this.velocityPlugin = velocityPlugin;
		}

		Platform detectPlatform() {
			return Platforms.velocity();
		}

		BaseWrapper create() {
			Platform platform = detectPlatform();
			ProxyServer server = velocityPlugin.server;
			Path folder = velocityPlugin.folder;
			Logger logger = velocityPlugin.logger;

			PluginContainer plugin = server.getPluginManager().fromInstance(velocityPlugin).get();
			Scheduler scheduler = server.getScheduler();
			Executor executor = (cmd) -> scheduler.buildTask(plugin, cmd).schedule();

			Path jarFile = plugin.getDescription().getSource().get();
			LibertyBansLauncher launcher = new LibertyBansLauncher(
					new Slf4jBootstrapLogger(velocityPlugin.logger), platform,
					velocityPlugin.folder, executor, jarFile);
			ClassLoader launchLoader = launcher.attemptLaunch().join();

			if (launchLoader == null) {
				logger.warn("Failed to launch LibertyBans");
				return null;
			}
			BaseFoundation base;
			try {
				base = new Instantiator("space.arim.libertybans.env.velocity.VelocityLauncher", launchLoader)
						.invoke(Map.Entry.class, entry(plugin, server), folder);
			} catch (IllegalArgumentException | SecurityException | ReflectiveOperationException ex) {
				logger.warn("Failed to launch LibertyBans", ex);
				return null;
			}
			base.startup();
			return new BaseWrapper(base);
		}
		
	}
	
	private static <K, V> Map.Entry<K, V> entry(K key, V value) {
		return new java.util.AbstractMap.SimpleImmutableEntry<>(key, value);
	}
	
	void close() {
		base.shutdown();
	}
	
}
