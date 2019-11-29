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

import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.events.bukkit.PostPunishEvent;
import space.arim.bans.api.events.bukkit.PostUnpunishEvent;
import space.arim.bans.api.events.bukkit.PunishEvent;
import space.arim.bans.api.events.bukkit.UnpunishEvent;
import space.arim.bans.api.exception.MissingCenterException;
import space.arim.bans.api.exception.MissingPunishmentException;
import space.arim.bans.env.Enforcer;

public class BukkitEnforcer implements Enforcer {

	private final BukkitEnv environment;
	
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
	
	void enforceBans(AsyncPlayerPreLoginEvent evt) {
		if (environment.center() == null) {
			enforceFailed(evt.getName(), PunishmentType.BAN);
			return;
		}
		if (!evt.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
			return;
		} else if (environment.center().isBanned(environment.center().subjects().parseSubject(evt.getUniqueId()))) {
			try {
				evt.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, environment.center().formats()
						.formatPunishment(environment.center().punishments().getPunishment(environment.center().subjects().parseSubject(evt.getUniqueId()), PunishmentType.BAN)));
			} catch (MissingPunishmentException ex) {
				environment.center().logError(ex);
			}
		} else {
			List<String> ips = environment.center().resolver().getIps(evt.getUniqueId());
			ips.add(evt.getAddress().getHostAddress());
			for (String addr : ips) {
				if (environment.center().isBanned(environment.center().subjects().parseSubject(addr))) {
					try {
						evt.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
								environment.center().formats().formatPunishment(environment.center().punishments().getPunishment(environment.center().subjects().parseSubject(addr), PunishmentType.BAN)));
					} catch (MissingPunishmentException ex) {
						environment.center().logError(ex);
					}
				}
			}
		}
	}

	void enforceMutes(AsyncPlayerChatEvent evt) {
		if (environment.center() == null) {
			enforceFailed(evt.getPlayer().getName(), PunishmentType.MUTE);
			return;
		}
		if (evt.isCancelled()) {
			return;
		} else if (environment.center().isMuted(environment.center().subjects().parseSubject(evt.getPlayer().getUniqueId()))) {
			evt.setCancelled(true);
			try {
				environment.center().subjects().sendMessage(environment.center().subjects().parseSubject(evt.getPlayer().getUniqueId()), environment.center().formats().formatPunishment(environment.center().punishments().getPunishment(environment.center().subjects().parseSubject(evt.getPlayer().getUniqueId()), PunishmentType.MUTE)));
			} catch (MissingPunishmentException ex) {
				environment.center().logError(ex);
			}
		} else {
			for (String addr : environment.center().resolver().getIps(evt.getPlayer().getUniqueId())) {
				if (environment.center().isBanned(environment.center().subjects().parseSubject(addr))) {
					evt.setCancelled(true);
					try {
						environment.center().subjects().sendMessage(environment.center().subjects().parseSubject(evt.getPlayer().getUniqueId()), environment.center().formats().formatPunishment(environment.center().punishments().getPunishment(environment.center().subjects().parseSubject(addr), PunishmentType.MUTE)));
					} catch (MissingPunishmentException ex) {
						environment.center().logError(ex);
					}
				}
			}
		}
	}
	
	void updateCache(AsyncPlayerPreLoginEvent evt) {
		if (environment.center() == null) {
			 cacheFailed(evt.getName());
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

}
