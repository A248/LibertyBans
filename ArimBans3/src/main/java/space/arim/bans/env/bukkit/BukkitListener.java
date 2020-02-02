/*
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.env.bukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import space.arim.bans.internal.Configurable;

public class BukkitListener implements Configurable, Listener {
	
	private final BukkitEnv environment;
	
	public BukkitListener(final BukkitEnv environment) {
		this.environment = environment;
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	private void cacheData(AsyncPlayerPreLoginEvent evt) {
		environment.enforcer().updateCache(evt);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	private void enforceBansHighest(AsyncPlayerPreLoginEvent evt) {
		environment.enforcer().enforceBans(evt, EventPriority.HIGHEST);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	private void enforceBansHigh(AsyncPlayerPreLoginEvent evt) {
		environment.enforcer().enforceBans(evt, EventPriority.HIGH);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	private void enforceBansNormal(AsyncPlayerPreLoginEvent evt) {
		environment.enforcer().enforceBans(evt, EventPriority.NORMAL);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	private void enforceBansLow(AsyncPlayerPreLoginEvent evt) {
		environment.enforcer().enforceBans(evt, EventPriority.LOW);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void enforceBansLowest(AsyncPlayerPreLoginEvent evt) {
		environment.enforcer().enforceBans(evt, EventPriority.LOWEST);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	private void enforceMutesHighest(AsyncPlayerChatEvent evt) {
		environment.enforcer().enforceMutes(evt, EventPriority.HIGHEST);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	private void enforceMutesHigh(AsyncPlayerChatEvent evt) {
		environment.enforcer().enforceMutes(evt, EventPriority.HIGH);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	private void enforceMutesNormal(AsyncPlayerChatEvent evt) {
		environment.enforcer().enforceMutes(evt, EventPriority.NORMAL);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	private void enforceMutesLow(AsyncPlayerChatEvent evt) {
		environment.enforcer().enforceMutes(evt, EventPriority.LOW);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void enforceMutesLowest(AsyncPlayerChatEvent evt) {
		environment.enforcer().enforceMutes(evt, EventPriority.LOWEST);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	private void enforceMutesCmdsHighest(PlayerCommandPreprocessEvent evt) {
		environment.enforcer().enforceMutes(evt, EventPriority.HIGHEST);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	private void enforceMutesCmdsHigh(PlayerCommandPreprocessEvent evt) {
		environment.enforcer().enforceMutes(evt, EventPriority.HIGH);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	private void enforceMutesCmdsNormal(PlayerCommandPreprocessEvent evt) {
		environment.enforcer().enforceMutes(evt, EventPriority.NORMAL);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	private void enforceMutesCmdsLow(PlayerCommandPreprocessEvent evt) {
		environment.enforcer().enforceMutes(evt, EventPriority.LOW);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void enforceMutesCmdsLowest(PlayerCommandPreprocessEvent evt) {
		environment.enforcer().enforceMutes(evt, EventPriority.LOWEST);
	}
	
}
