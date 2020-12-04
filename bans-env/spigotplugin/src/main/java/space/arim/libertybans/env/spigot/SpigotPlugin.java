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

import org.bukkit.plugin.java.JavaPlugin;

import space.arim.libertybans.bootstrap.logger.JavaVersionDetection;
import space.arim.libertybans.bootstrap.logger.JulBootstrapLogger;

public class SpigotPlugin extends JavaPlugin {

	private BaseWrapper wrapper;
	
	@Override
	public synchronized void onEnable() {
		JulBootstrapLogger logger = new JulBootstrapLogger(getLogger());
		if (!new JavaVersionDetection(logger).detectVersion()) {
			return;
		}
		if (wrapper != null) {
			throw new IllegalStateException("Plugin enabled twice?");
		}
		wrapper = new BaseWrapper.Creator(this, logger).create();
	}
	
	@Override
	public synchronized void onDisable() {
		BaseWrapper wrapper = this.wrapper;
		this.wrapper = null;
		if (wrapper == null) {
			getLogger().warning("LibertyBans wasn't launched; check your log for a startup error");
			return;
		}
		wrapper.close();
	}
	
}
