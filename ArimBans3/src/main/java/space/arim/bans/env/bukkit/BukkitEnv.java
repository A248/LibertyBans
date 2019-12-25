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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.bstats.bukkit.Metrics;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import space.arim.bans.ArimBans;
import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.Subject;
import space.arim.bans.api.Subject.SubjectType;
import space.arim.bans.api.exception.InvalidSubjectException;
import space.arim.bans.api.exception.PlayerNotFoundException;
import space.arim.bans.api.util.minecraft.MinecraftUtil;
import space.arim.bans.env.Environment;

public class BukkitEnv implements Environment {
	
	private final JavaPlugin plugin;
	private final Set<EnvLibrary> libraries = loadLibraries();
	private ArimBans center;
	private final BukkitEnforcer enforcer;
	private final BukkitListener listener;
	private final BukkitCommands commands;
	
	private boolean registered = false;

	public BukkitEnv(JavaPlugin plugin) {
		this.plugin = plugin;
		this.enforcer = new BukkitEnforcer(this);
		this.listener = new BukkitListener(this);
		this.commands = new BukkitCommands(this);
	}
	
	@Override
	public void loadFor(ArimBans center) {
		this.center = center;
		if (!registered) {
			plugin.getServer().getPluginManager().registerEvents(listener, plugin);
			plugin.getServer().getPluginCommand("arimbans").setExecutor(commands);
			setupMetrics();
			registered = true;
		}
	}
	
	@Override
	public boolean isOnlineMode() {
		return plugin.getServer().getOnlineMode();
	}
	
	@Override
	public void shutdown(String message) {
		plugin.getLogger().severe("*** ArimBans Severe Error ***\nShutting down because: " + message);
		close();
		plugin.getServer().getPluginManager().disablePlugin(plugin);
	}
	
	private void setupMetrics() {
		Metrics metrics = new Metrics(plugin);
		metrics.addCustomChart(new Metrics.SimplePie("storage_mode", () -> center.sql().getStorageModeName()));
		metrics.addCustomChart(new Metrics.SimplePie("json_messages", () -> Boolean.toString(center.formats().useJson())));
	}
	
	static void sendMessage(Player target, String jsonable, boolean useJson) {
		if (useJson) {
			target.spigot().sendMessage(MinecraftUtil.parseJson(jsonable));
		} else {
			target.sendMessage(jsonable);
		}
	}

	@Override
	public boolean isOnline(Subject subj) {
		if (subj.getType().equals(SubjectType.PLAYER)) {
			for (Player check : plugin.getServer().getOnlinePlayers()) {
				if (subj.getUUID().equals(check.getUniqueId())) {
					return true;
				}
			}
			return false;
		} else if (subj.getType().equals(SubjectType.IP)) {
			for (Player check : plugin.getServer().getOnlinePlayers()) {
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
			Player target = plugin.getServer().getPlayer(subj.getUUID());
			if (target != null) {
				sendMessage(target, jsonable, useJson);
			}
		} else if (subj.getType().equals(SubjectType.CONSOLE)) {
			plugin.getServer().getConsoleSender().sendMessage(MinecraftUtil.stripJson(jsonable));
		} else if (subj.getType().equals(SubjectType.IP)) {
			for (Player target : applicable(subj)) {
				sendMessage(target, jsonable, useJson);
			}
		}
	}
	
	@Override
	public void sendMessage(String permission, String jsonable, boolean useJson) {
		for (Player player : plugin.getServer().getOnlinePlayers()) {
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
			OfflinePlayer target = plugin.getServer().getOfflinePlayer(subject.getUUID());
			if (target != null) {
				return target.isOp() ? opPerms : target.getPlayer().hasPermission(permission);
			}
			throw new InvalidSubjectException("Subject " + center.formats().formatSubject(subject) + " does not have a valid UUID.");
		} else if (subject.getType().equals(SubjectType.IP)) {
			throw new InvalidSubjectException("Cannot invoke Environment#hasPermission(Subject, Permission[]) for IP-based subjects");
		}
		throw new InvalidSubjectException("Subject type is completely missing!");
	}
	
	public Set<? extends Player> applicable(Subject subject) {
		Set<Player> applicable = new HashSet<Player>();
		if (subject.getType().equals(SubjectType.PLAYER)) {
			for (Player check : plugin.getServer().getOnlinePlayers()) {
				if (subject.getUUID().equals(check.getUniqueId())) {
					applicable.add(check);
				}
			}
		} else if (subject.getType().equals(SubjectType.IP)) {
			for (Player check : plugin.getServer().getOnlinePlayers()) {
				if (center.resolver().hasIp(check.getUniqueId(), subject.getIP())) {
					applicable.add(check);
				}
			}
		}
		return applicable;
	}
	
	@Override
	public UUID uuidFromName(String name) throws PlayerNotFoundException {
		for (final OfflinePlayer player : plugin.getServer().getOfflinePlayers()) {
			if (player.getName().equalsIgnoreCase(name)) {
				return player.getUniqueId();
			}
		}
		throw new PlayerNotFoundException(name);
	}
	
	@Override
	public String nameFromUUID(UUID uuid) throws PlayerNotFoundException {
		for (final OfflinePlayer player : plugin.getServer().getOfflinePlayers()) {
			if (player.getUniqueId().equals(uuid)) {
				return player.getName();
			}
		}
		throw new PlayerNotFoundException(uuid);
	}
	
	public JavaPlugin plugin() {
		return plugin;
	}
	
	public ArimBans center() {
		return center;
	}

	@Override
	public void enforce(Punishment punishment, boolean useJson) {
		enforcer().enforce(punishment, useJson);
	}
	
	BukkitEnforcer enforcer() {
		return enforcer;
	}

	@Override
	public Logger logger() {
		return plugin.getLogger();
	}
	
	@Override
	public String getName() {
		return plugin.getName();
	}
	
	@Override
	public String getAuthor() {
		return plugin.getDescription().getAuthors().get(0);
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
			HandlerList.unregisterAll(listener);
		}
		commands.close();
		listener.close();
		enforcer.close();
	}

}
