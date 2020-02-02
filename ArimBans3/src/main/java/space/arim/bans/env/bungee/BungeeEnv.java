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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bstats.bungeecord.Metrics;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.Subject;
import space.arim.bans.api.Subject.SubjectType;
import space.arim.bans.api.exception.InvalidSubjectException;
import space.arim.bans.env.Environment;

import space.arim.universal.util.collections.CollectionsUtil;

import space.arim.api.server.bungee.BungeeUtil;

public class BungeeEnv implements Environment {

	private final Plugin plugin;
	private ArimBans center;
	private final BungeeEnforcer enforcer;
	private final BungeeListener listener;
	private final BungeeCommands commands;
	
	private boolean registered = false;
	
	public BungeeEnv(Plugin plugin) {
		this.plugin = plugin;
		this.enforcer = new BungeeEnforcer(this);
		this.listener = new BungeeListener(this);
		this.commands = new BungeeCommands(this);
	}
	
	@Override
	public void loadFor(ArimBans center) {
		this.center = center;
		if (!registered) {
			plugin.getProxy().getPluginManager().registerListener(plugin, listener);
			plugin.getProxy().getPluginManager().registerCommand(plugin, commands);
			Metrics metrics = new Metrics(plugin, 5991);
			metrics.addCustomChart(new Metrics.SimplePie("storage_mode", () -> center.sql().getStorageModeName()));
			metrics.addCustomChart(new Metrics.SimplePie("json_messages", () -> Boolean.toString(center.formats().useJson())));
			registered = true;
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOnlineMode() {
		return plugin.getProxy().getConfig().isOnlineMode();
	}
	
	void sendMessage(ProxiedPlayer target, String jsonable, boolean useJson) {
		if (useJson) {
			target.sendMessage(BungeeUtil.parseJson(jsonable));
		} else {
			target.sendMessage(BungeeUtil.colour(jsonable));
		}
	}
	
	void sendConsoleMessage(String jsonable) {
		plugin.getProxy().getConsole().sendMessage(BungeeUtil.colour(BungeeUtil.stripJson(jsonable)));
	}
	
	@Override
	public boolean isOnline(Subject subject) {
		switch (subject.getType()) {
		case PLAYER:
			return plugin.getProxy().getPlayer(subject.getUUID()) != null;
		case IP:
			return plugin.getProxy().getPlayers().stream().anyMatch((player) -> center.resolver().hasIp(player.getUniqueId(), subject.getIP()));
		default:
			return subject.getType().equals(SubjectType.CONSOLE);
		}
	}
	
	@Override
	public void sendMessage(Subject subj, String jsonable, boolean useJson) {
		if (subj.getType().equals(SubjectType.PLAYER)) {
			ProxiedPlayer target = plugin.getProxy().getPlayer(subj.getUUID());
			if (target != null) {
				sendMessage(target, jsonable, useJson);
			}
		} else if (subj.getType().equals(SubjectType.CONSOLE)) {
			sendConsoleMessage(jsonable);
		} else if (subj.getType().equals(SubjectType.IP)) {
			applicable(subj).forEach((target) -> sendMessage(target, jsonable, useJson));
		}
	}
	
	@Override
	public void sendMessage(String permission, String jsonable, boolean useJson) {
		plugin.getProxy().getPlayers().forEach((player) -> {
			if (player.hasPermission(permission)) {
				sendMessage(player, jsonable, useJson);
			}
		});
	}
	
	private boolean checkOfflinePlayerPermission(UUID uuid, String permission) {
		ProxiedPlayer target = plugin.getProxy().getPlayer(uuid);
		return target != null ? target.hasPermission(permission) : CollectionsUtil.checkForAnyMatches(plugin.getProxy().getConfigurationAdapter().getGroups(uuid.toString()), (group) -> plugin.getProxy().getConfigurationAdapter().getPermissions(group).contains(permission));
	}
	
	@Override
	public boolean hasPermission(Subject subject, String permission, boolean opPerms) {
		if (subject.getType().equals(SubjectType.CONSOLE)) {
			return true;
		} else if (subject.getType().equals(SubjectType.PLAYER)) {
			return checkOfflinePlayerPermission(subject.getUUID(), permission);
		} else if (subject.getType().equals(SubjectType.IP)) {
			return center.resolver().getPlayers(subject.getIP()).stream().anyMatch((uuid) -> checkOfflinePlayerPermission(uuid, permission));
		}
		throw new InvalidSubjectException("Subject type is completely missing!");
	}
	
	Set<ProxiedPlayer> applicable(Subject subject) {
		switch (subject.getType()) {
		case PLAYER:
			Set<ProxiedPlayer> applicable1 = new HashSet<ProxiedPlayer>();
			plugin.getProxy().getPlayers().forEach((check) -> {
				if (subject.getUUID().equals(check.getUniqueId())) {
					applicable1.add(check);
				}
			});
			return applicable1;
		case IP:
			Set<ProxiedPlayer> applicable2 = new HashSet<ProxiedPlayer>();
			plugin.getProxy().getPlayers().forEach((check) -> {
				if (center.resolver().hasIp(check.getUniqueId(), subject.getIP())) {
					applicable2.add(check);
				}
			});
			return applicable2;
		default:
			return Collections.emptySet();
		}
	}
	
	@Override
	public UUID uuidFromName(String name) {
		for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
			if (player.getName().equals(name)) {
				return player.getUniqueId();
			}
		}
		return null;
	}
	
	@Override
	public String nameFromUUID(UUID uuid) {
		for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
			if (player.getUniqueId().equals(uuid)) {
				return player.getName();
			}
		}
		return null;
	}
	
	public Plugin plugin() {
		return plugin;
	}
	
	public ArimBans center() {
		return center;
	}
	
	@Override
	public void enforce(Punishment punishment, boolean useJson) {
		enforcer().enforce(punishment, useJson);
	}
	
	BungeeEnforcer enforcer() {
		return enforcer;
	}
	
	@Override
	public Logger logger() {
		return plugin.getLogger();
	}

	@Override
	public String getName() {
		return plugin.getDescription().getName();
	}
	
	@Override
	public String getAuthor() {
		return plugin.getDescription().getAuthor();
	}
	
	@Override
	public String getVersion() {
		return plugin.getDescription().getVersion();
	}
	
	@Override
	public void refreshConfig(boolean first) {
		commands.refreshConfig(first);
		listener.refreshConfig(first);
		enforcer.refreshConfig(first);
	}
	
	@Override
	public void refreshMessages(boolean first) {
		commands.refreshMessages(first);
		listener.refreshMessages(first);
		enforcer.refreshMessages(first);
	}
	
	@Override
	public void close() {
		if (registered) {
			plugin.getProxy().getPluginManager().unregisterListener(listener);
			plugin.getProxy().getPluginManager().unregisterCommand(commands);
		}
		commands.close();
		listener.close();
		enforcer.close();
	}

}
