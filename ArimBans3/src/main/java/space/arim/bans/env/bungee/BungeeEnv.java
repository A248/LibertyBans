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

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.Subject;
import space.arim.bans.api.Subject.SubjectType;
import space.arim.bans.api.exception.InvalidSubjectException;
import space.arim.bans.api.exception.MissingCacheException;
import space.arim.bans.env.Environment;

import space.arim.universal.util.collections.CollectionsUtil;
import space.arim.universal.util.collections.ErringCollectionsUtil;

import space.arim.api.uuid.PlayerNotFoundException;
import space.arim.api.util.minecraft.MinecraftUtil;

public class BungeeEnv implements Environment {

	private final Plugin plugin;
	private final Set<EnvLibrary> libraries = loadLibraries();
	private ArimBans center;
	private final BungeeEnforcer enforcer;
	private final BungeeCommands commands;
	
	private boolean registered = false;
	
	public BungeeEnv(Plugin plugin) {
		this.plugin = plugin;
		this.enforcer = new BungeeEnforcer(this);
		this.commands = new BungeeCommands(this);
	}
	
	@Override
	public void loadFor(ArimBans center) {
		this.center = center;
		if (!registered) {
			plugin.getProxy().getPluginManager().registerListener(plugin, enforcer);
			plugin.getProxy().getPluginManager().registerCommand(plugin, commands);
			Metrics metrics = new Metrics(plugin);
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
	
	@Override
	public void shutdown(String message) {
		plugin.getLogger().severe("*** ArimBans Severe Error ***\nShutting down because: " + message);
		close();
	}
	
	static void sendMessage(ProxiedPlayer target, String jsonable, boolean useJson) {
		if (useJson) {
			target.sendMessage(MinecraftUtil.parseJson(jsonable));
		} else {
			target.sendMessage(convert(jsonable));
		}
	}
	
	@Override
	public boolean isOnline(Subject subj) {
		switch (subj.getType()) {
		case PLAYER:
			return CollectionsUtil.checkForAnyMatches(plugin.getProxy().getPlayers(), (check) -> subj.getUUID().equals(check.getUniqueId()));
		case IP:
			return CollectionsUtil.checkForAnyMatches(plugin.getProxy().getPlayers(), (check) -> center.resolver().hasIp(check.getUniqueId(), subj.getIP()));
		default:
			return subj.getType().equals(SubjectType.CONSOLE);
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
			plugin.getProxy().getConsole().sendMessage(convert(MinecraftUtil.stripJson(jsonable)));
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
	
	@Override
	public boolean hasPermission(Subject subject, String permission, boolean opPerms) {
		if (subject.getType().equals(SubjectType.CONSOLE)) {
			return true;
		} else if (subject.getType().equals(SubjectType.PLAYER)) {
			ProxiedPlayer target = plugin.getProxy().getPlayer(subject.getUUID());
			if (target != null) {
				return target.hasPermission(permission);
			}
			return CollectionsUtil.checkForAnyMatches(plugin.getProxy().getConfigurationAdapter().getGroups(subject.getUUID().toString()), (group) -> plugin.getProxy().getConfigurationAdapter().getPermissions(group).contains(permission));
		} else if (subject.getType().equals(SubjectType.IP)) {
			try {
				return ErringCollectionsUtil.<UUID, MissingCacheException>checkForAnyMatches(center.resolver().getPlayers(subject.getIP()), (uuid) -> {
					ProxiedPlayer target = plugin.getProxy().getPlayer(uuid);
					return target != null ? target.hasPermission(permission) : ErringCollectionsUtil.<String, MissingCacheException>checkForAnyMatches(plugin.getProxy().getConfigurationAdapter().getGroups(center.resolver().getName(uuid)), (group) -> plugin.getProxy().getConfigurationAdapter().getPermissions(group).contains(permission));
				});
			} catch (MissingCacheException ex) {
				throw new InvalidSubjectException("One of the names of an ip-based subject could not be resolved", ex);
			}
		}
		throw new InvalidSubjectException("Subject type is completely missing!");
	}
	
	public Set<ProxiedPlayer> applicable(Subject subject) {
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
	public UUID uuidFromName(String name) throws PlayerNotFoundException {
		for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
			if (player.getName().equals(name)) {
				return player.getUniqueId();
			}
		}
		throw new PlayerNotFoundException(name);
	}
	
	@Override
	public String nameFromUUID(UUID uuid) throws PlayerNotFoundException {
		for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
			if (player.getUniqueId().equals(uuid)) {
				return player.getName();
			}
		}
		throw new PlayerNotFoundException(uuid);
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
	public boolean isLibrarySupported(EnvLibrary type) {
		return libraries.contains(type);
	}
	
	@Override
	public void refreshConfig(boolean first) {
		commands.refreshConfig(first);
		enforcer.refreshConfig(first);
	}
	
	@Override
	public void refreshMessages(boolean first) {
		commands.refreshMessages(first);
		enforcer.refreshMessages(first);
	}
	
	@Override
	public void close() {
		if (registered) {
			plugin.getProxy().getPluginManager().unregisterListener(enforcer);
			plugin.getProxy().getPluginManager().unregisterCommand(commands);
		}
		commands.close();
		enforcer.close();
	}
	
	static BaseComponent[] convert(String input) {
		return TextComponent.fromLegacyText(input);
	}

}
