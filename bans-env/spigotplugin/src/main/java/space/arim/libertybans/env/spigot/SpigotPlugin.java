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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bukkit.plugin.java.JavaPlugin;

import space.arim.libertybans.bootstrap.BaseEnvironment;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;

public class SpigotPlugin extends JavaPlugin {

	private BaseEnvironment base;
	
	@Override
	public void onEnable() {
		File folder = getDataFolder();
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		LibertyBansLauncher launcher = new LibertyBansLauncher(folder, executor, (clazz) -> {
			try {
				JavaPlugin potential = JavaPlugin.getProvidingPlugin(clazz);
				return potential.getDescription().getFullName();
			} catch (IllegalArgumentException ignored) {}
			return null;
		});
		ClassLoader launchLoader = launcher.attemptLaunch().join();
		executor.shutdown();
		if (launchLoader == null) {
			// Already printed the error message
			setEnabled(false);
			return;
		}
		Class<?> envClazz;
		try {
			envClazz = Class.forName("space.arim.libertybans.env.spigot.SpigotEnv", true, launchLoader);
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
			setEnabled(false);
			return;
		}
		try {
			base = (BaseEnvironment) envClazz.getDeclaredConstructor(JavaPlugin.class).newInstance(this);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException ex) {
			ex.printStackTrace();
			setEnabled(false);
			return;
		}
		base.startup();
	}
	
	@Override
	public void onDisable() {
		if (base != null) {
			base.shutdown();
		}
	}
	
}
