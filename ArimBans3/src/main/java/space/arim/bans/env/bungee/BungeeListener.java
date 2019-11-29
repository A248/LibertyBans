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
package space.arim.bans.env.bungee;

import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import space.arim.bans.internal.Configurable;

public class BungeeListener implements Configurable, Listener {
	
	private final BungeeEnv environment;
	
	public BungeeListener(BungeeEnv environment) {
		this.environment = environment;
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	private void cacheData(LoginEvent evt) {
		environment.enforcer().updateCache(evt);
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	private void enforceBansHighest(LoginEvent evt) {
		environment.enforcer().enforceBans(evt, EventPriority.HIGHEST);
	}
	
	@EventHandler(priority=EventPriority.HIGH)
	private void enforceBansHigh(LoginEvent evt) {
		environment.enforcer().enforceBans(evt, EventPriority.HIGH);
	}
	
	@EventHandler(priority=EventPriority.NORMAL)
	private void enforceBansNormal(LoginEvent evt) {
		environment.enforcer().enforceBans(evt, EventPriority.NORMAL);
	}
	
	@EventHandler(priority=EventPriority.LOW)
	private void enforceBansLow(LoginEvent evt) {
		environment.enforcer().enforceBans(evt, EventPriority.LOW);
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	private void enforceBansLowest(LoginEvent evt) {
		environment.enforcer().enforceBans(evt, EventPriority.LOWEST);
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	private void enforceMutesHighest(ChatEvent evt) {
		environment.enforcer().enforceMutes(evt, EventPriority.HIGHEST);
	}
	
	@EventHandler(priority=EventPriority.HIGH)
	private void enforceMutesHigh(ChatEvent evt) {
		environment.enforcer().enforceMutes(evt, EventPriority.HIGH);
	}
	
	@EventHandler(priority=EventPriority.NORMAL)
	private void enforceMutesNormal(ChatEvent evt) {
		environment.enforcer().enforceMutes(evt, EventPriority.NORMAL);
	}
	
	@EventHandler(priority=EventPriority.LOW)
	private void enforceMutesLow(ChatEvent evt) {
		environment.enforcer().enforceMutes(evt, EventPriority.LOW);
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	private void enforceMutesLowest(ChatEvent evt) {
		environment.enforcer().enforceMutes(evt, EventPriority.LOWEST);
	}
	
}
