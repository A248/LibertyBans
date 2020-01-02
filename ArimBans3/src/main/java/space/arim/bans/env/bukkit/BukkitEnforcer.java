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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentResult;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.exception.MissingCenterException;
import space.arim.bans.internal.Configurable;

public class BukkitEnforcer implements Configurable, Listener {

	private final BukkitEnv environment;
	
	private final ConcurrentHashMap<AsyncPlayerPreLoginEvent, Future<PunishmentResult>> logins = new ConcurrentHashMap<AsyncPlayerPreLoginEvent, Future<PunishmentResult>>();
	//private final ConcurrentHashMap<AsyncPlayerChatEvent, Future<PunishmentResult>> chats = new ConcurrentHashMap<AsyncPlayerChatEvent, Future<PunishmentResult>>();

	
	public BukkitEnforcer(final BukkitEnv environment) {
		this.environment = Objects.requireNonNull(environment, "Environment must not be null!");
	}
	
	private void missingCenter(String message) {
		environment.logger().warning("Warning! Are you restarting ArimBans?");
		(new MissingCenterException(message)).printStackTrace();
	}
	
	private void enforceFailed(String subject, PunishmentType type) {
		missingCenter(subject + " was not checked for " + type.toString());
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	private void enforceBansStart(AsyncPlayerPreLoginEvent evt) {
		if (environment.center() == null) {
			enforceFailed(evt.getName(), PunishmentType.BAN);
			return;
		}
		logins.put(evt, environment.center().submit(() -> {
			UUID uuid = evt.getUniqueId();
			String address = evt.getAddress().getHostAddress();
			environment.center().resolver().update(uuid, evt.getName(), address);
			return environment.center().corresponder().getApplicablePunishment(uuid, address, PunishmentType.BAN);
		}));
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	private void enforceBans(AsyncPlayerPreLoginEvent evt) {
		if (environment.center() == null) {
			enforceFailed(evt.getName(), PunishmentType.BAN);
			return;
		}
		if (!evt.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
			logins.remove(evt);
			return;
		} else if (logins.containsKey(evt)) {
			try {
				PunishmentResult result = logins.get(evt).get();
				if (result.hasPunishment()) {
					evt.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, result.getApplicableMessage());
				}
			} catch (InterruptedException | ExecutionException ex) {
				environment.center().logs().logError(ex);
			}
		}
	}
	
	private void enforceMutes(Cancellable evt, EventPriority priority, Player player) {
		if (evt.isCancelled()) {
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

	void enforce(Punishment punishment, boolean useJson) {
		Set<? extends Player> targets = environment.applicable(punishment.subject());
		String message = environment.center().formats().formatPunishment(punishment);
		if (punishment.type().equals(PunishmentType.BAN) || punishment.type().equals(PunishmentType.MUTE)) {
			environment.plugin().getServer().getScheduler().runTask(environment.plugin(), () -> targets.forEach((target) -> target.kickPlayer(message)));
		} else if (punishment.type().equals(PunishmentType.MUTE) || punishment.type().equals(PunishmentType.WARN)) {
			targets.forEach((target) -> BukkitEnv.sendMessage(target, message, useJson));
		}
	}
	
}
