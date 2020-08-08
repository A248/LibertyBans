/* 
 * LibertyBans-env-bungeeplugin
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-bungeeplugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-env-bungeeplugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with LibertyBans-env-bungeeplugin. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.libertybans.env.bungee;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;

import space.arim.libertybans.bootstrap.BaseEnvironment;
import space.arim.libertybans.bootstrap.Instantiator;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;

public class BungeePlugin extends Plugin {

	private BaseEnvironment base;
	
	@Override
	public void onEnable() {
		/*
		 * It's only BungeeCord which has this problem
		 */
		informAboutSecurityManagerIfNeeded();

		Path libsFolder = getDataFolder().toPath().resolve("libs");
		ExecutorService executor = Instantiator.createReasonableExecutor();
		ClassLoader launchLoader;
		try {
			LibertyBansLauncher launcher = new LibertyBansLauncher(libsFolder, executor, (clazz) -> {
				try {
					ClassLoader pluginClassLoader = clazz.getClassLoader();
					Field descField = pluginClassLoader.getClass().getDeclaredField("desc");
					Object descObj = descField.get(pluginClassLoader);
					if (descObj instanceof PluginDescription) {
						PluginDescription desc = (PluginDescription) descObj;
						return desc.getName() + " v" + desc.getVersion();
					}
				} catch (IllegalArgumentException | NoSuchFieldException | SecurityException | IllegalAccessException ignored) {}
				return null;
			});
			launchLoader = launcher.attemptLaunch().join();
		} finally {
			executor.shutdown();
			assert executor.isTerminated();
		}
		if (launchLoader == null) {
			getLogger().warning("Failed to launch LibertyBans");
			return;
		}
		BaseEnvironment base;
		try {
			base = new Instantiator("space.arim.libertybans.env.bungee.BungeeEnv", launchLoader).invoke(Plugin.class, this);
		} catch (IllegalArgumentException | SecurityException | ReflectiveOperationException ex) {
			getLogger().log(Level.WARNING, "Failed to launch LibertyBans", ex);
			return;
		}
		base.startup();
		this.base = base;
	}
	
	private void informAboutSecurityManagerIfNeeded() {
		Logger logger = getLogger();
		SecurityManager sm = System.getSecurityManager();
		if (sm == null) {
			logger.info("Thank you for using Waterfall and not BungeeCord. Waterfall helps us and you.");

		} else {
			logger.warning(
					"You are using BungeeCord and its 'BungeeSecurityManager' is enabled. LibertyBans requires permissions "
					+ "to create reasonable thread pools per efficient connecting pooling. You may encounter some console spam.");
		}
	}

	@Override
	public void onDisable() {
		BaseEnvironment base = this.base;
		if (base == null) {
			getLogger().warning("LibertyBans wasn't launched; check your log for a startup error");
			return;
		}
		base.shutdown();
	}
	
}
