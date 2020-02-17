/*
 * ArimBans3, a punishment plugin for minecraft servers
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * ArimBans3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans3. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.env.sponge;

import java.util.logging.Level;
import java.util.stream.Stream;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentResult;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.exception.ConfigSectionException;
import space.arim.bans.api.exception.MissingCenterException;
import space.arim.bans.internal.Configurable;

import space.arim.api.concurrent.SyncExecution;
import space.arim.api.server.sponge.SpongeUtil;

public class SpongeEnforcer implements Configurable {

	private final SpongeEnv environment;
	
	private Order ban_priority;
	private Order mute_priority;
	
	public SpongeEnforcer(SpongeEnv environment) {
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
	
	void enforceBans(ClientConnectionEvent.Auth evt, Order priority) {
		if (environment.center() == null) {
			enforceFailed(evt.getProfile().getName().orElse("unknown"), PunishmentType.BAN);
			return;
		}
		if (evt.isCancelled() || !priority.equals(ban_priority)) {
			return;
		}
		PunishmentResult result = environment.center().corresponder().getApplicablePunishment(evt.getProfile().getUniqueId(), evt.getConnection().getAddress().getAddress().getHostAddress(), PunishmentType.BAN);
		if (result.hasPunishment()) {
			evt.setMessage(SpongeUtil.colour(result.getApplicableMessage()));
			evt.setCancelled(true);
		}
	}
	
	private void enforceMutes(Cancellable evt, Order priority, Player player) {
		if (evt.isCancelled() || !priority.equals(mute_priority)) {
			return;
		}
		PunishmentResult result = environment.center().corresponder().getApplicablePunishment(player.getUniqueId(), player.getConnection().getAddress().getAddress().getHostAddress(), PunishmentType.MUTE);
		if (result.hasPunishment()) {
			evt.setCancelled(true);
			environment.sendMessage(player, result.getApplicableMessage(), environment.center().formats().useJson());
		}
	}
	
	void enforceMutes(MessageChannelEvent.Chat evt, Order priority, Player player) {
		if (environment.center() == null) {
			enforceFailed(player.getName(), PunishmentType.MUTE);
			return;
		}
		enforceMutes((Cancellable) evt, priority, player);
	}
	
	void enforceMutes(SendCommandEvent evt, Order priority, Player player) {
		if (environment.center() == null) {
			enforceFailed(player.getName(), PunishmentType.MUTE);
			return;
		}
		if (environment.center().formats().isCmdMuteBlocked(evt.getCommand())) {
			enforceMutes((Cancellable) evt, priority, player);
		}
	}
	
	void updateCache(ClientConnectionEvent.Auth evt) {
		if (environment.center() == null) {
			 cacheFailed(evt.getProfile().getName().orElse("unknown"));
			 return;
		}
		if (evt.getProfile().getName().isPresent()) {
			environment.center().resolver().update(evt.getProfile().getUniqueId(), evt.getProfile().getName().get(), evt.getConnection().getAddress().getAddress().getHostAddress());
		} else {
			environment.center().logs().log(Level.CONFIG, "Profile " + evt.getProfile().getUniqueId() + " has no corresponding name!");
		}
	}
	
	void enforce(Punishment punishment, boolean useJson) {
		Stream<Player> targets = environment.applicable(punishment.subject());
		String message = environment.center().formats().formatPunishment(punishment);
		if (punishment.type().equals(PunishmentType.BAN) || punishment.type().equals(PunishmentType.KICK)) {
			environment.center().getRegistry().getRegistration(SyncExecution.class).execute(() -> targets.forEach((target) -> target.kick(SpongeUtil.colour(message))));
		} else if (punishment.type().equals(PunishmentType.MUTE) || punishment.type().equals(PunishmentType.WARN)) {
			targets.forEach((target) -> environment.sendMessage(target, message, useJson));
		}
	}
	
	private Order parsePriority(String key) {
		switch (environment.center().config().getConfigString(key).toLowerCase()) {
		case "highest":
			return Order.LAST;
		case "high":
			return Order.LATE;
		case "normal":
			return Order.DEFAULT;
		case "low":
			return Order.EARLY;
		case "lowest":
			return Order.FIRST;
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
