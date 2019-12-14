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
import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.Subject;
import space.arim.bans.api.Subject.SubjectType;
import space.arim.bans.api.exception.InvalidSubjectException;
import space.arim.bans.api.exception.PlayerNotFoundException;
import space.arim.bans.api.util.minecraft.MinecraftUtil;
import space.arim.bans.env.Environment;

public class BungeeEnv implements Environment {

	private Plugin plugin;
	private final Set<EnvLibrary> libraries = loadLibraries();
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
	
	public void setCenter(ArimBans center) {
		this.center = center;
		if (!registered) {
			plugin.getProxy().getPluginManager().registerListener(plugin, listener);
			plugin.getProxy().getPluginManager().registerCommand(plugin, commands);
			setupMetrics();
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
	
	private void setupMetrics() {
		Metrics metrics = new Metrics(plugin);
		metrics.addCustomChart(new Metrics.SimplePie("storage_mode", () -> center.sql().getStorageModeName()));
		metrics.addCustomChart(new Metrics.SimplePie("json_messages", () -> Boolean.toString(center.formats().useJson())));
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
		if (subj.getType().equals(SubjectType.PLAYER)) {
			for (ProxiedPlayer check : plugin.getProxy().getPlayers()) {
				if (subj.getUUID().equals(check.getUniqueId())) {
					return true;
				}
			}
			return false;
		} else if (subj.getType().equals(SubjectType.IP)) {
			for (ProxiedPlayer check : plugin.getProxy().getPlayers()) {
				if (center.resolver().hasIp(check.getUniqueId(), subj.getIP())) {
					return true;
				}
			}
			return false;
		}
		return subj.getType().equals(SubjectType.CONSOLE);
	}
	
	@Override
	public void sendMessage(Subject subj, String jsonable, boolean useJson) {
		ArimBansLibrary.checkString(jsonable);
		if (subj.getType().equals(SubjectType.PLAYER)) {
			ProxiedPlayer target = plugin.getProxy().getPlayer(subj.getUUID());
			if (target != null) {
				sendMessage(target, jsonable, useJson);
			}
		} else if (subj.getType().equals(SubjectType.CONSOLE)) {
			plugin.getProxy().getConsole().sendMessage(convert(MinecraftUtil.stripJson(jsonable)));
		} else if (subj.getType().equals(SubjectType.IP)) {
			for (ProxiedPlayer target : applicable(subj)) {
				sendMessage(target, jsonable, useJson);
			}
		}
	}
	
	@Override
	public void sendMessage(String permission, String jsonable, boolean useJson) {
		for (ProxiedPlayer player : plugin.getProxy().getPlayers()) {
			if (player.hasPermission(permission)) {
				sendMessage(player, jsonable, useJson);
			}
		}
	}

	@Override
	public boolean hasPermission(Subject subject, String permission, boolean opPerms) {
		ArimBansLibrary.checkString(permission);
		if (subject.getType().equals(SubjectType.CONSOLE)) {
			return true;
		} else if (subject.getType().equals(SubjectType.PLAYER)) {
			ProxiedPlayer target = plugin.getProxy().getPlayer(subject.getUUID());
			if (target != null) {
				return target.hasPermission(permission);
			}
			throw new InvalidSubjectException("Subject " + center.formats().formatSubject(subject) + " is not online.");
		} else if (subject.getType().equals(SubjectType.IP)) {
			throw new InvalidSubjectException("Cannot invoke Environment#hasPermission(Subject, Permission[]) for IP-based subjects");
		}
		throw new InvalidSubjectException("Subject type is completely missing!");
	}
	
	public Set<ProxiedPlayer> applicable(Subject subject) {
		Set<ProxiedPlayer> applicable = new HashSet<ProxiedPlayer>();
		if (subject.getType().equals(SubjectType.PLAYER)) {
			for (ProxiedPlayer check : plugin.getProxy().getPlayers()) {
				if (subject.getUUID().equals(check.getUniqueId())) {
					applicable.add(check);
				}
			}
		} else if (subject.getType().equals(SubjectType.IP)) {
			for (ProxiedPlayer check : plugin.getProxy().getPlayers()) {
				if (center.resolver().hasIp(check.getUniqueId(), subject.getIP())) {
					applicable.add(check);
				}
			}
		}
		return applicable;
	}
	
	@Override
	public UUID uuidFromName(String name) throws PlayerNotFoundException {
		for (final ProxiedPlayer player : plugin.getProxy().getPlayers()) {
			if (player.getName().equals(name)) {
				return player.getUniqueId();
			}
		}
		throw new PlayerNotFoundException(name);
	}
	
	@Override
	public String nameFromUUID(UUID uuid) throws PlayerNotFoundException {
		for (final ProxiedPlayer player : plugin.getProxy().getPlayers()) {
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
	public BungeeEnforcer enforcer() {
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
	public void close() {
		if (registered) {
			plugin.getProxy().getPluginManager().unregisterListener(listener);
			plugin.getProxy().getPluginManager().unregisterCommand(commands);
		}
		commands.close();
		listener.close();
		enforcer.close();
	}
	
	static BaseComponent[] convert(String input) {
		return TextComponent.fromLegacyText(input);
	}

}
