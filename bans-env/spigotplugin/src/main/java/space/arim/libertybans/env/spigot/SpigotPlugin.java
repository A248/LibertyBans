/* 
 * LibertyBans-env-spigotplugin
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-spigotplugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-env-spigotplugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with LibertyBans-env-spigotplugin. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.libertybans.env.spigot;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import space.arim.libertybans.bootstrap.BaseEnvironment;
import space.arim.libertybans.bootstrap.Instantiator;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;

public class SpigotPlugin extends JavaPlugin {

	private BaseEnvironment base;
	
	@Override
	public void onEnable() {
		Path folder = getDataFolder().toPath();
		ExecutorService executor = Executors.newFixedThreadPool(8);
		ClassLoader launchLoader;
		try {
			LibertyBansLauncher launcher = new LibertyBansLauncher(folder, executor, (clazz) -> {
				try {
					JavaPlugin potential = JavaPlugin.getProvidingPlugin(clazz);
					return potential.getDescription().getFullName();
				} catch (IllegalArgumentException ignored) {}
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
			base = new Instantiator("space.arim.libertybans.env.spigot.SpigotEnv", launchLoader)
					.invoke(JavaPlugin.class, this, Path.class, folder);
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
			getLogger().warning("LibertyBans never launched; nothing to shutdown");
			return;
		}
		base.shutdown();
	}
	
}
