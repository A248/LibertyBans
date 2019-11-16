package space.arim.bans.env.bungee;

import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import space.arim.bans.api.exception.ConfigSectionException;

public class BungeeListener implements Listener {
	
	private BungeeEnv environment;
	
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
		switch (environment.center().config().getString(key).toLowerCase()) {
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
	
	public void refreshConfig() {
		ban_priority = parsePriority("bans.event-priority");
		mute_priority = parsePriority("mutes.event-priority");
	}
}
