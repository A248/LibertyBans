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

import java.util.HashSet;
import java.util.Set;

import net.md_5.bungee.api.plugin.Plugin;

import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.PunishmentPlugin;
import space.arim.bans.api.util.minecraft.bungee.BungeeUtil;
import space.arim.bans.extended.bungee.CommandSkeleton;

import space.arim.universal.registry.UniversalRegistry;

public class ArimBansExtendedBungee extends Plugin implements ArimBansExtendedPluginBase {

	private ArimBansExtended extended = null;
	private Set<CommandSkeleton> cmds = new HashSet<CommandSkeleton>();
	
	private void shutdown(String message) {
		getLogger().warning("ArimBansExtended shutting down! Reason: " + message);
		throw new IllegalStateException("Shuttind down...");
	}
	
	@Override
	public void onEnable() {
		try {
			Class.forName("space.arim.bans.api.ArimBansLibrary");
			Class.forName("space.arim.universal.registry.UniversalRegistry");
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
		loadCmds();
	}
	
	private void loadCmds() {
		for (String cmd : ArimBansExtended.commands()) {
			cmds.add(new CommandSkeleton(this, cmd));
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
	
	public Set<String> getTabComplete(String[] args) {
		return BungeeUtil.getPlayerNameTabComplete(args, getProxy());
	}
	
}
