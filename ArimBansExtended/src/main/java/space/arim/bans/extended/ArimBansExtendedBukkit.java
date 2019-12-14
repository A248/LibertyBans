/* 
 * ArimBansExtended, an extension for the ArimBans core
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansExtended is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansExtended is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansExtended. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.extended;

import org.bukkit.plugin.java.JavaPlugin;

import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.PunishmentPlugin;
import space.arim.bans.extended.bukkit.CommandListener;
import space.arim.registry.UniversalRegistry;

public class ArimBansExtendedBukkit extends JavaPlugin implements ArimBansExtendedPluginBase {

	private ArimBansExtended extended = null;
	private CommandListener cmds;
	
	private void shutdown(String message) {
		getLogger().warning("ArimBansExtended shutting down! Reason: " + message);
		getServer().getPluginManager().disablePlugin(this);
		throw new IllegalStateException("Shutting down...");
	}
	
	@Override
	public void onEnable() {
		try {
			Class.forName("space.arim.bans.api.ArimBansLibrary");
			Class.forName("space.arim.registry.UniversalRegistry");
		} catch (ClassNotFoundException ex) {
			shutdown("ArimBansLibrary / UniversalRegistry not on classpath!");
			return;
		}
		PunishmentPlugin plugin = UniversalRegistry.getRegistration(PunishmentPlugin.class);
		if (plugin != null) {
			if (plugin instanceof ArimBansLibrary) {
				extended = new ArimBansExtended((ArimBansLibrary) plugin, getDataFolder(), getLogger());
			} else {
				shutdown("PunishmentPlugin is not an instance of ArimBansLibrary.");
			}
		} else {
			shutdown("No PunishmentPlugin's registered!");
		}
		cmds = new CommandListener(this);
		registerCommands();
	}
	
	private void registerCommands() {
		for (String cmd : ArimBansExtended.commands()) {
			getServer().getPluginCommand(cmd).setExecutor(cmds);
		}
	}
	
	@Override
	public void onDisable() {
		close();
	}
	
	@Override
	public ArimBansExtended extension() {
		return extended;
	}

}
