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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.events.bukkit.PostPunishEvent;
import space.arim.bans.api.events.bukkit.PostUnpunishEvent;
import space.arim.bans.api.events.bukkit.PunishEvent;
import space.arim.bans.api.events.bukkit.UnpunishEvent;
import space.arim.bans.api.exception.ConfigSectionException;
import space.arim.bans.api.exception.MissingCenterException;
import space.arim.bans.api.exception.MissingPunishmentException;
import space.arim.bans.env.Enforcer;

public class BukkitEnforcer implements Enforcer {

	private final BukkitEnv environment;
	
	private EventPriority ban_priority;
	private EventPriority mute_priority;
	private boolean strict_ip_checking;
	
	public BukkitEnforcer(final BukkitEnv environment) {
		this.environment = environment;
	}
	
	private void missingCenter(String message) {
		environment.logger().warning("MissingCenterException! Are you restarting ArimBans?");
		(new MissingCenterException(message)).printStackTrace();
	}
	
	private void cacheFailed(String subject) {
		missingCenter(subject + "'s information was not updated");
	}
	
	private void enforceFailed(String subject, PunishmentType type) {
		missingCenter(subject + " was not checked for " + type.toString());
	}
	
	void enforceBans(AsyncPlayerPreLoginEvent evt, EventPriority priority) {
		if (environment.center() == null) {
			enforceFailed(evt.getName(), PunishmentType.BAN);
			return;
		}
		if (!evt.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED) || !priority.equals(ban_priority)) {
			return;
		}
		Subject subject = environment.center().subjects().parseSubject(evt.getUniqueId());
		if (environment.center().isBanned(subject)) {
			try {
				evt.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, environment.center().formats()
						.formatPunishment(environment.center().punishments().getPunishment(subject, PunishmentType.BAN)));
			} catch (MissingPunishmentException ex) {
				environment.center().logError(ex);
			}
		} else if (strict_ip_checking) {
			List<String> ips = new ArrayList<String>(environment.center().resolver().getIps(evt.getUniqueId()));
			ips.add(evt.getAddress().getHostAddress());
			for (String addr : ips) {
				Subject addrSubj = environment.center().subjects().parseSubject(addr);
				if (environment.center().isBanned(addrSubj)) {
					try {
						evt.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, environment.center().formats().formatPunishment(environment.center().punishments().getPunishment(addrSubj, PunishmentType.BAN)));
					} catch (MissingPunishmentException ex) {
						environment.center().logError(ex);
					}
				}
			}
		} else {
			Subject addrSubj = environment.center().subjects().parseSubject(evt.getAddress().getHostAddress());
			if (environment.center().isBanned(addrSubj)) {
				try {
					evt.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, environment.center().formats().formatPunishment(environment.center().punishments().getPunishment(addrSubj, PunishmentType.BAN)));
				} catch (MissingPunishmentException ex) {
					environment.center().logError(ex);
				}
			}
		}
	}
	
	private void enforceMutes(Cancellable evt, EventPriority priority, Player player) {
		if (evt.isCancelled() || !priority.equals(mute_priority)) {
			return;
		}
		Subject subject = environment.center().subjects().parseSubject(player.getUniqueId());
		if (environment.center().isMuted(subject)) {
			try {
				environment.center().subjects().sendMessage(subject, environment.center().formats().formatPunishment(environment.center().punishments().getPunishment(subject, PunishmentType.MUTE)));
				evt.setCancelled(true);
			} catch (MissingPunishmentException ex) {
				environment.center().logError(ex);
			}
		} else if (strict_ip_checking) {
			for (String addr : environment.center().resolver().getIps(subject.getUUID())) {
				Subject addrSubj = environment.center().subjects().parseSubject(addr);
				if (environment.center().isMuted(addrSubj)) {
					try {
						environment.center().subjects().sendMessage(subject, environment.center().formats().formatPunishment(environment.center().punishments().getPunishment(addrSubj, PunishmentType.MUTE)));
						evt.setCancelled(true);
					} catch (MissingPunishmentException ex) {
						environment.center().logError(ex);
					}
				}
			}
		} else {
			Subject addrSubj = environment.center().subjects().parseSubject(player.getAddress().getAddress().getHostAddress());
			if (environment.center().isMuted(addrSubj)) {
				try {
					environment.center().subjects().sendMessage(subject, environment.center().formats().formatPunishment(environment.center().punishments().getPunishment(addrSubj, PunishmentType.MUTE)));
					evt.setCancelled(true);
				} catch (MissingPunishmentException ex) {
					environment.center().logError(ex);
				}
			}
		}
	}

	void enforceMutes(AsyncPlayerChatEvent evt, EventPriority priority) {
		if (environment.center() == null) {
			enforceFailed(evt.getPlayer().getName(), PunishmentType.MUTE);
			return;
		}
		enforceMutes(evt, priority, evt.getPlayer());
	}
	
	void enforceMutes(PlayerCommandPreprocessEvent evt, EventPriority priority) {
		if (environment.center() == null) {
			enforceFailed(evt.getPlayer().getName(), PunishmentType.MUTE);
			return;
		}
		if (environment.center().formats().isCmdMuteBlocked(evt.getMessage())) {
			enforceMutes(evt, priority, evt.getPlayer());
		}
	}
	
	void updateCache(AsyncPlayerPreLoginEvent evt) {
		if (environment.center() == null) {
			 cacheFailed(evt.getName());
			 return;
		}
		environment.center().resolver().update(evt.getUniqueId(), evt.getName(), evt.getAddress().getHostAddress());
	}

	@Override
	public void enforce(Punishment punishment, boolean useJson) {
		Set<? extends Player> targets = environment.applicable(punishment.subject());
		String message = environment.center().formats().formatPunishment(punishment);
		if (punishment.type().equals(PunishmentType.BAN) || punishment.type().equals(PunishmentType.MUTE)) {
			environment.plugin().getServer().getScheduler().runTask(environment.plugin(), () -> {
				for (Player target : targets) {
					target.kickPlayer(message);
				}
			});
		} else if (punishment.type().equals(PunishmentType.MUTE) || punishment.type().equals(PunishmentType.WARN)) {
			for (Player target : targets) {
				if (useJson) {
					environment.json(target, message);
				} else {
					target.sendMessage(message);
				}
			}
		}
	}
	
	@Override
	public boolean callPunishEvent(Punishment punishment, boolean retro) {
		PunishEvent  evt = new PunishEvent(punishment, retro);
		environment.plugin().getServer().getPluginManager().callEvent(evt);
		return !evt.isCancelled();
	}
	
	@Override
	public boolean callUnpunishEvent(Punishment punishment, boolean automatic) {
		UnpunishEvent evt = new UnpunishEvent(punishment, automatic);
		environment.plugin().getServer().getPluginManager().callEvent(evt);
		// Must return true if event is automatic
		// Otherwise data gets corrupted
		return automatic || !evt.isCancelled();
	}

	@Override
	public void callPostPunishEvent(Punishment punishment, boolean retro) {
		environment.plugin().getServer().getPluginManager().callEvent(new PostPunishEvent(punishment, retro));
	}

	@Override
	public void callPostUnpunishEvent(Punishment punishment, boolean automatic) {
		environment.plugin().getServer().getPluginManager().callEvent(new PostUnpunishEvent(punishment, automatic));
	}

	private EventPriority parsePriority(String key) {
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
			return null;
		default:
			throw new ConfigSectionException(key);
		}
	}
	
	@Override
	public void refreshConfig(boolean fromFile) {
		ban_priority = parsePriority("enforcement.priorities.event-priority");
		mute_priority = parsePriority("enforcement.priorities.event-priority");
		strict_ip_checking = environment.center().config().getConfigBoolean("enforcement.strict-ip-checking");
	}
	
}
