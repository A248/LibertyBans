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

import java.util.Objects;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentResult;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.exception.ConfigSectionException;
import space.arim.bans.api.exception.MissingCenterException;
import space.arim.bans.internal.Configurable;

import space.arim.api.concurrent.SyncExecutor;

public class BukkitEnforcer implements Configurable {

	private final BukkitEnv environment;
	
	private EventPriority ban_priority;
	private EventPriority mute_priority;

	
	public BukkitEnforcer(final BukkitEnv environment) {
		this.environment = Objects.requireNonNull(environment, "Environment must not be null!");
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
		PunishmentResult result = environment.center().corresponder().getApplicablePunishment(evt.getUniqueId(), evt.getAddress().getHostAddress(), PunishmentType.BAN);
		if (result.hasPunishment()) {
			evt.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, result.getApplicableMessage());
		}
	}
	
	private void enforceMutes(Cancellable evt, EventPriority priority, Player player) {
		if (evt.isCancelled() || !priority.equals(mute_priority)) {
			return;
		}
		PunishmentResult result = environment.center().corresponder().getApplicablePunishment(player.getUniqueId(), player.getAddress().getAddress().getHostAddress(), PunishmentType.MUTE);
		if (result.hasPunishment()) {
			evt.setCancelled(true);
			BukkitEnv.sendMessage(player, result.getApplicableMessage(), environment.center().formats().useJson());
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

	void enforce(Punishment punishment, boolean useJson) {
		Set<? extends Player> targets = environment.applicable(punishment.subject());
		String message = environment.center().formats().formatPunishment(punishment);
		if (punishment.type().equals(PunishmentType.BAN) || punishment.type().equals(PunishmentType.MUTE)) {
			environment.center().getRegistry().getRegistration(SyncExecutor.class).execute(() -> targets.forEach((target) -> target.kickPlayer(message)));
		} else if (punishment.type().equals(PunishmentType.MUTE) || punishment.type().equals(PunishmentType.WARN)) {
			targets.forEach((target) -> BukkitEnv.sendMessage(target, message, useJson));
		}
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
	public void refreshConfig(boolean first) {
		ban_priority = parsePriority("enforcement.priorities.event-priority");
		mute_priority = parsePriority("enforcement.priorities.event-priority");
	}
	
}
