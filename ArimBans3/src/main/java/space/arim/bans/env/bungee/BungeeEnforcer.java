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

import java.util.Objects;
import java.util.Set;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.event.EventPriority;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentResult;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.exception.ConfigSectionException;
import space.arim.bans.api.exception.MissingCenterException;
import space.arim.bans.env.Enforcer;

public class BungeeEnforcer implements Enforcer {

	private final BungeeEnv environment;
	
	private byte ban_priority;
	private byte mute_priority;
	
	public BungeeEnforcer(BungeeEnv environment) {
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
	
	void enforceBans(LoginEvent evt, byte priority) {
		if (environment.center() == null) {
			enforceFailed(evt.getConnection().getName(), PunishmentType.BAN);
			return;
		}
		if (evt.isCancelled() || priority != ban_priority) {
			return;
		}
		PunishmentResult result = environment.center().corresponder().getApplicablePunishment(evt.getConnection().getUniqueId(), evt.getConnection().getAddress().getAddress().getHostAddress(), PunishmentType.BAN);
		if (result.hasPunishment()) {
			evt.setCancelReason(BungeeEnv.convert(result.getApplicableMessage()));
			evt.setCancelled(true);
		}
	}
	
	void enforceMutes(ChatEvent evt, byte priority) {
		if (!(evt.getSender() instanceof ProxiedPlayer) || evt.isCancelled() || priority != mute_priority || evt.isCommand() && !environment.center().formats().isCmdMuteBlocked(evt.getMessage())) {
			return;
		}
		ProxiedPlayer player = (ProxiedPlayer) evt.getSender();
		if (environment.center() == null) {
			enforceFailed(player.getName(), PunishmentType.MUTE);
			return;
		}
		PunishmentResult result = environment.center().corresponder().getApplicablePunishment(player.getUniqueId(), player.getAddress().getAddress().getHostAddress(), PunishmentType.MUTE);
		if (result.hasPunishment()) {
			evt.setCancelled(true);
			BungeeEnv.sendMessage(player, result.getApplicableMessage(), environment.center().formats().useJson());
		}
	}
	
	void updateCache(LoginEvent evt) {
		if (environment.center() == null) {
			 cacheFailed(evt.getConnection().getName());
			 return;
		}
		environment.center().resolver().update(evt.getConnection().getUniqueId(), evt.getConnection().getName(), evt.getConnection().getAddress().getAddress().getHostAddress());
	}

	@Override
	public void enforce(Punishment punishment, boolean useJson) {
		Set<ProxiedPlayer> targets = environment.applicable(punishment.subject());
		String message = environment.center().formats().formatPunishment(punishment);
		if (punishment.type().equals(PunishmentType.BAN) || punishment.type().equals(PunishmentType.MUTE)) {
			for (ProxiedPlayer target : targets) {
				target.disconnect(BungeeEnv.convert(message));
			}
		} else if (punishment.type().equals(PunishmentType.MUTE) || punishment.type().equals(PunishmentType.WARN)) {
			for (ProxiedPlayer target : targets) {
				BungeeEnv.sendMessage(target, message, useJson);
			}
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
	public void refreshConfig(boolean fromFile) {
		ban_priority = parsePriority("enforcement.priorities.event-priority");
		mute_priority = parsePriority("enforcement.priorities.event-priority");
	}
	
}
