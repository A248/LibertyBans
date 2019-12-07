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
package space.arim.bans.extended.bukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import space.arim.bans.api.PunishmentResult;
import space.arim.bans.extended.ArimBansExtendedBukkit;

public class SignListener implements Listener {
	
	private final ArimBansExtendedBukkit plugin;
	
	public SignListener(ArimBansExtendedBukkit plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority=EventPriority.LOW)
	public void onSignEdit(SignChangeEvent evt) {
		if (plugin.enabled()) {
			PunishmentResult result = plugin.extension().getLib().getApplicableMute(evt.getPlayer().getUniqueId(), evt.getPlayer().getAddress().getAddress().getHostAddress());
			if (result.hasPunishment()) {
				evt.setCancelled(true);
				plugin.extension().getLib().sendMessage(evt.getPlayer().getUniqueId(), result.getApplicableMessage());
			}
		}
	}
	
}
