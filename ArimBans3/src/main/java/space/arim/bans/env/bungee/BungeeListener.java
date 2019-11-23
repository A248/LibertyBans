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
import space.arim.bans.api.exception.ConfigSectionException;
import space.arim.bans.internal.Configurable;

public class BungeeListener implements Configurable, Listener {
	
	private final BungeeEnv environment;
	
	private byte ban_priority;
	private byte mute_priority;
	public BungeeListener(BungeeEnv environment) {
		this.environment = environment;
		refreshConfig();
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	private void cacheData(LoginEvent evt) {
		environment.enforcer().updateCache(evt);
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	private void enforceBansHighest(LoginEvent evt) {
		if (EventPriority.HIGHEST == ban_priority) {
			environment.enforcer().enforceBans(evt);
		}
	}
	
	@EventHandler(priority=EventPriority.HIGH)
	private void enforceBansHigh(LoginEvent evt) {
		if (EventPriority.HIGH == ban_priority) {
			environment.enforcer().enforceBans(evt);
		}
	}
	
	@EventHandler(priority=EventPriority.NORMAL)
	private void enforceBansNormal(LoginEvent evt) {
		if (EventPriority.NORMAL == ban_priority) {
			environment.enforcer().enforceBans(evt);
		}
	}
	
	@EventHandler(priority=EventPriority.LOW)
	private void enforceBansLow(LoginEvent evt) {
		if (EventPriority.LOW == ban_priority) {
			environment.enforcer().enforceBans(evt);
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	private void enforceBansLowest(LoginEvent evt) {
		if (EventPriority.LOWEST == ban_priority) {
			environment.enforcer().enforceBans(evt);
		}
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	private void enforceMutesHighest(ChatEvent evt) {
		if (EventPriority.HIGHEST == mute_priority) {
			environment.enforcer().enforceMutes(evt);
		}
	}
	
	@EventHandler(priority=EventPriority.HIGH)
	private void enforceMutesHigh(ChatEvent evt) {
		if (EventPriority.HIGH == mute_priority) {
			environment.enforcer().enforceMutes(evt);
		}
	}
	
	@EventHandler(priority=EventPriority.NORMAL)
	private void enforceMutesNormal(ChatEvent evt) {
		if (EventPriority.NORMAL == mute_priority) {
			environment.enforcer().enforceMutes(evt);
		}
	}
	
	@EventHandler(priority=EventPriority.LOW)
	private void enforceMutesLow(ChatEvent evt) {
		if (EventPriority.LOW == mute_priority) {
			environment.enforcer().enforceMutes(evt);
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	private void enforceMutesLowest(ChatEvent evt) {
		if (EventPriority.LOWEST == mute_priority) {
			environment.enforcer().enforceMutes(evt);
		}
	}
	
	private byte parsePriority(String key) {
		switch (environment.center().config().getConfigString(key).toLowerCase()) {
		case "highest":
			return EventPriority.HIGHEST;
		case "high":
			return EventPriority.HIGH;
		case "normal":
			return EventPriority.NORMAL;
		case "low":
			return EventPriority.LOW;
		case "lowest":
			return EventPriority.LOWEST;
		case "none":
			return (byte) -2;
		default:
			throw new ConfigSectionException(key);
		}
	}
	
	@Override
	public void refreshConfig() {
		ban_priority = parsePriority("bans.event-priority");
		mute_priority = parsePriority("mutes.event-priority");
	}
	
}
