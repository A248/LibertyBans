package space.arim.bans.env.bukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import space.arim.bans.api.exception.ConfigSectionException;

public class BukkitListener implements AutoCloseable, Listener {
	private final BukkitEnv environment;
	
	private EventPriority ban_priority;
	private EventPriority mute_priority;
	public BukkitListener(final BukkitEnv environment) {
		this.environment = environment;
		refreshConfig();
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	private void cacheData(AsyncPlayerPreLoginEvent evt) {
		environment.enforcer().updateCache(evt);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	private void enforceBansHighest(AsyncPlayerPreLoginEvent evt) {
		if (EventPriority.HIGHEST.equals(ban_priority)) {
			environment.enforcer().enforceBans(evt);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	private void enforceBansHigh(AsyncPlayerPreLoginEvent evt) {
		if (EventPriority.HIGH.equals(ban_priority)) {
			environment.enforcer().enforceBans(evt);
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	private void enforceBansNormal(AsyncPlayerPreLoginEvent evt) {
		if (EventPriority.NORMAL.equals(ban_priority)) {
			environment.enforcer().enforceBans(evt);
		}
	}
	
	@EventHandler(priority = EventPriority.LOW)
	private void enforceBansLow(AsyncPlayerPreLoginEvent evt) {
		if (EventPriority.LOW.equals(ban_priority)) {
			environment.enforcer().enforceBans(evt);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void enforceBansLowest(AsyncPlayerPreLoginEvent evt) {
		if (EventPriority.LOWEST.equals(ban_priority)) {
			environment.enforcer().enforceBans(evt);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	private void enforceMutesHighest(AsyncPlayerChatEvent evt) {
		if (EventPriority.HIGHEST.equals(mute_priority)) {
			environment.enforcer().enforceMutes(evt);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	private void enforceMutesHigh(AsyncPlayerChatEvent evt) {
		if (EventPriority.HIGH.equals(mute_priority)) {
			environment.enforcer().enforceMutes(evt);
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	private void enforceMutesNormal(AsyncPlayerChatEvent evt) {
		if (EventPriority.NORMAL.equals(mute_priority)) {
			environment.enforcer().enforceMutes(evt);
		}
	}
	
	@EventHandler(priority = EventPriority.LOW)
	private void enforceMutesLow(AsyncPlayerChatEvent evt) {
		if (EventPriority.LOW.equals(mute_priority)) {
			environment.enforcer().enforceMutes(evt);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void enforceMutesLowest(AsyncPlayerChatEvent evt) {
		if (EventPriority.LOWEST.equals(mute_priority)) {
			environment.enforcer().enforceMutes(evt);
		}
	}

	private EventPriority parsePriority(String key) {
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
			return null;
		default:
			throw new ConfigSectionException(key);
		}
	}
	
	public void refreshConfig() {
		ban_priority = parsePriority("bans.event-priority");
		mute_priority = parsePriority("mutes.event-priority");
	}
	
	@Override
	public void close() throws Exception {
		
	}
}
