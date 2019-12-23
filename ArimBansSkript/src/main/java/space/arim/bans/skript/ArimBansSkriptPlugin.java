/* 
 * ArimBansSkript, a skript addon for ArimBans
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansSkript is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansSkript is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansSkript. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.skript;

import org.bukkit.plugin.java.JavaPlugin;

import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.PunishmentPlugin;

import space.arim.universal.registry.UniversalRegistry;

public class ArimBansSkriptPlugin extends JavaPlugin {

	private ArimBansSkript center;
	
	@Override
	public void onEnable() {
		PunishmentPlugin plugin = UniversalRegistry.get().getRegistration(PunishmentPlugin.class);
		if (plugin instanceof ArimBansLibrary) {
			center = new ArimBansSkript((ArimBansLibrary) plugin, getLogger());
			center.registerAll();
		}
	}
	
	@Override
	public void onDisable() {
		if (center != null) {
			center.close();
		}
	}
	
}
