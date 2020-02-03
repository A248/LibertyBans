/*
 * ArimBansExtended, an extension for the ArimBans core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
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
package space.arim.bans.extended.sponge;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.filter.cause.First;

import space.arim.bans.api.PunishmentResult;
import space.arim.bans.extended.ArimBansExtendedPlugin;

public class SignListener {

	private final ArimBansExtendedPlugin plugin;
	
	public SignListener(ArimBansExtendedPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Listener
	private void onSignEdit(ChangeSignEvent evt, @First Player player) {
		if (!evt.isCancelled() && plugin.enabled() && plugin.extension().antiSignEnabled()) {
			PunishmentResult result = plugin.extension().getLib().getApplicableMute(player.getUniqueId(), player.getConnection().getAddress().getAddress().getHostAddress());
			if (result.hasPunishment()) {
				evt.setCancelled(true);
				plugin.extension().getLib().sendMessage(player.getUniqueId(), result.getApplicableMessage());
			}
		}
	}
	
}
