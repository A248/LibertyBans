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

import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.logging.Level;

import space.arim.libertybans.bootstrap.BaseEnvironment;
import space.arim.libertybans.bootstrap.Instantiator;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.TaskScheduler;

public class BungeePlugin extends Plugin {

	private BaseEnvironment base;
	
	@Override
	public void onEnable() {
		Path folder = getDataFolder().toPath();
		TaskScheduler scheduler = getProxy().getScheduler();
		Executor executor = (cmd) -> scheduler.runAsync(this, cmd);

		LibertyBansLauncher launcher = new LibertyBansLauncher(folder, executor, (clazz) -> {
			return PluginClassLoaderReflection.getProvidingPlugin(clazz);
		});
		ClassLoader launchLoader = launcher.attemptLaunch().join();

		if (launchLoader == null) {
			getLogger().warning("Failed to launch LibertyBans");
			return;
		}
		BaseEnvironment base;
		try {
			base = new Instantiator("space.arim.libertybans.env.bungee.BungeeEnv", launchLoader)
					.invoke(Plugin.class, this, Path.class, folder);
		} catch (IllegalArgumentException | SecurityException | ReflectiveOperationException ex) {
			getLogger().log(Level.WARNING, "Failed to launch LibertyBans", ex);
			return;
		}
		base.startup();
		this.base = base;
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
