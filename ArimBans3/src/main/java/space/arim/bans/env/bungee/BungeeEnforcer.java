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

import java.util.List;
import java.util.Set;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.event.EventPriority;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.events.bungee.PostUnpunishEvent;
import space.arim.bans.api.events.bungee.PostPunishEvent;
import space.arim.bans.api.events.bungee.PunishEvent;
import space.arim.bans.api.events.bungee.UnpunishEvent;
import space.arim.bans.api.exception.ConfigSectionException;
import space.arim.bans.api.exception.MissingCenterException;
import space.arim.bans.api.exception.MissingPunishmentException;
import space.arim.bans.env.Enforcer;

public class BungeeEnforcer implements Enforcer {

	private final BungeeEnv environment;
	
	private byte ban_priority;
	private byte mute_priority;
	
	public BungeeEnforcer(BungeeEnv environment) {
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
	
	void enforceBans(LoginEvent evt, byte priority) {
		if (environment.center() == null) {
			enforceFailed(evt.getConnection().getName(), PunishmentType.BAN);
			return;
		}
		if (evt.isCancelled() || priority != ban_priority) {
			return;
		} else if (environment.center().isBanned(environment.center().subjects().parseSubject(evt.getConnection().getUniqueId()))) {
			try {
				evt.setCancelled(true);
				evt.setCancelReason(environment.convert(environment.center().formats().formatPunishment(environment.center().punishments().getPunishment(environment.center().subjects().parseSubject(evt.getConnection().getUniqueId()), PunishmentType.BAN))));
			} catch (MissingPunishmentException ex) {
				environment.center().logError(ex);
			}
		} else {
			List<String> ips = environment.center().resolver().getIps(evt.getConnection().getUniqueId());
			ips.add(evt.getConnection().getAddress().getAddress().getHostAddress());
			for (String addr : ips) {
				if (environment.center().isBanned(environment.center().subjects().parseSubject(addr))) {
					try {
						evt.setCancelled(true);
						evt.setCancelReason(environment.convert(environment.center().formats().formatPunishment(environment.center().punishments().getPunishment(environment.center().subjects().parseSubject(addr), PunishmentType.BAN))));
					} catch (MissingPunishmentException ex) {
						environment.center().logError(ex);
					}
				}
			}
		}
	}
	
	void enforceMutes(ChatEvent evt, byte priority) {
		ProxiedPlayer player;
		if (evt.getSender() instanceof ProxiedPlayer) {
			player = (ProxiedPlayer) evt.getSender();
		} else {
			return;
		}
		if (environment.center() == null) {
			enforceFailed(player.getName(), PunishmentType.MUTE);
			return;
		}
		if (evt.isCancelled() || priority != mute_priority || evt.isCommand() && !environment.center().formats().isCmdMuteBlocked(evt.getMessage())) {
			return;
		}
		if (environment.center().isMuted(environment.center().subjects().parseSubject(player.getUniqueId()))) {
			evt.setCancelled(true);
			try {
				environment.center().subjects().sendMessage(environment.center().subjects().parseSubject(player.getUniqueId()), environment.center().formats().formatPunishment(environment.center().punishments().getPunishment(environment.center().subjects().parseSubject(player.getUniqueId()), PunishmentType.MUTE)));
			} catch (MissingPunishmentException ex) {
				environment.center().logError(ex);
			}
		} else {
			for (String addr : environment.center().resolver().getIps(player.getUniqueId())) {
				if (environment.center().isBanned(environment.center().subjects().parseSubject(addr))) {
					evt.setCancelled(true);
					try {
						environment.center().subjects().sendMessage(environment.center().subjects().parseSubject(addr), environment.center().formats().formatPunishment(environment.center().punishments().getPunishment(environment.center().subjects().parseSubject(addr), PunishmentType.MUTE)));
					} catch (MissingPunishmentException ex) {
						environment.center().logError(ex);
					}
				}
			}
		}
	}
	
	void updateCache(LoginEvent evt) {
		if (environment.center() == null) {
			 cacheFailed(evt.getConnection().getName());
		}
		environment.center().resolver().update(evt.getConnection().getUniqueId(), evt.getConnection().getName(), evt.getConnection().getAddress().getAddress().getHostAddress());
	}

	@Override
	public void enforce(Punishment punishment, boolean useJson) {
		Set<ProxiedPlayer> targets = environment.applicable(punishment.subject());
		String message = environment.center().formats().formatPunishment(punishment);
		if (punishment.type().equals(PunishmentType.BAN) || punishment.type().equals(PunishmentType.MUTE)) {
			for (ProxiedPlayer target : targets) {
				target.disconnect(environment.convert(message));
			}
		} else if (punishment.type().equals(PunishmentType.MUTE) || punishment.type().equals(PunishmentType.WARN)) {
			for (ProxiedPlayer target : targets) {
				if (useJson) {
					environment.json(target, message);
				} else {
					target.sendMessage(environment.convert(message));
				}
			}
		}
	}
	
	@Override
	public boolean callPunishEvent(Punishment punishment, boolean retro) {
		PunishEvent  evt = new PunishEvent(punishment, retro);
		environment.plugin().getProxy().getPluginManager().callEvent(evt);
		return !evt.isCancelled();
	}
	
	@Override
	public boolean callUnpunishEvent(Punishment punishment, boolean automatic) {
		UnpunishEvent evt = new UnpunishEvent(punishment, automatic);
		environment.plugin().getProxy().getPluginManager().callEvent(evt);
		// Must return true if event is automatic
		// Otherwise data gets corrupted
		return automatic || !evt.isCancelled();
	}

	@Override
	public void callPostPunishEvent(Punishment punishment, boolean retro) {
		environment.plugin().getProxy().getPluginManager().callEvent(new PostPunishEvent(punishment, retro));
	}

	@Override
	public void callPostUnpunishEvent(Punishment punishment, boolean automatic) {
		environment.plugin().getProxy().getPluginManager().callEvent(new PostUnpunishEvent(punishment, automatic));
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
	public void refreshConfig(boolean fromFile) {
		ban_priority = parsePriority("misc.priorities.event-priority");
		mute_priority = parsePriority("misc.priorities.event-priority");
	}
	
}
