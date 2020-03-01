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

import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.bstats.bukkit.Metrics;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.Subject;
import space.arim.bans.api.Subject.SubjectType;
import space.arim.bans.api.exception.InvalidSubjectException;
import space.arim.bans.env.Environment;

import space.arim.api.chat.FormattingCodePattern;
import space.arim.api.platform.spigot.SpigotMessages;

import net.milkbowl.vault.permission.Permission;

public class BukkitEnv implements Environment {
	
	private final Plugin plugin;
	private Permission permissions;
	private ArimBans center;
	private final BukkitEnforcer enforcer;
	private final BukkitListener listener;
	private final BukkitCommands commands;
	
	private boolean registered = false;
	
	public BukkitEnv(Plugin plugin) {
		this.plugin = plugin;
		this.enforcer = new BukkitEnforcer(this);
		this.listener = new BukkitListener(this);
		this.commands = new BukkitCommands(this);
	}
	
	@Override
	public void loadFor(ArimBans center) {
		this.center = center;
		if (!registered) {
			registered = true;
			if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
				RegisteredServiceProvider<Permission> registration = plugin.getServer().getServicesManager().getRegistration(Permission.class);
				if (registration != null) {
					permissions = registration.getProvider();
				}
	        }
			if (permissions == null) {
				center.logs().logBoth(Level.WARNING, "No Vault compatible permissions plugin installed. Punishment exemptions will not work for offline players.");
			}
			plugin.getServer().getPluginManager().registerEvents(listener, plugin);
			plugin.getServer().getPluginCommand("arimbans").setExecutor(commands);
			Metrics metrics = new Metrics(plugin, 5990);
			metrics.addCustomChart(new Metrics.SimplePie("storage_mode", () -> center.sql().getStorageModeName()));
			metrics.addCustomChart(new Metrics.SimplePie("json_messages", () -> Boolean.toString(center.formats().useJson())));
		}
	}
	
	@Override
	public boolean isOnlineMode() {
		return plugin.getServer().getOnlineMode();
	}
	
	void sendMessage(Player target, String jsonable, boolean useJson) {
		if (useJson) {
			target.spigot().sendMessage(SpigotMessages.get().parseJson(jsonable));
		} else {
			target.spigot().sendMessage(SpigotMessages.get().colour(jsonable));
		}
	}
	
	void sendConsoleMessage(String jsonable) {
		plugin.getServer().getConsoleSender().sendMessage(SpigotMessages.get().transformColourCodes(SpigotMessages.get().stripJson(jsonable), FormattingCodePattern.get(), '§'));
	}
	
	@Override
	public boolean isOnline(Subject subject) {
		switch (subject.getType()) {
		case PLAYER:
			return plugin.getServer().getPlayer(subject.getUUID()) != null;
		case IP:
			return plugin.getServer().getOnlinePlayers().stream().anyMatch((player) -> center.resolver().hasIp(player.getUniqueId(), subject.getIP()));
		default:
			return subject.getType().equals(SubjectType.CONSOLE);
		}
	}
	
	@Override
	public void sendMessage(Subject subj, String jsonable, boolean useJson) {
		if (subj.getType().equals(SubjectType.PLAYER)) {
			Player target = plugin.getServer().getPlayer(subj.getUUID());
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
		plugin.getServer().getOnlinePlayers().forEach((player) -> {
			if (player.hasPermission(permission)) {
				sendMessage(player, jsonable, useJson);
			}
		});
	}
	
	private boolean checkOfflinePlayerPermission(UUID uuid, String permission, boolean opPerms) {
		OfflinePlayer target = plugin.getServer().getOfflinePlayer(uuid);
		return target != null && (target.isOp() ? opPerms : target.isOnline() && target.getPlayer().hasPermission(permission) || permissions != null && permissions.playerHas(plugin.getServer().getWorlds().get(0).getName(), target, permission));
	}
	
	@Override
	public boolean hasPermission(Subject subject, String permission, boolean opPerms) {
		if (subject.getType().equals(SubjectType.CONSOLE)) {
			return true;
		} else if (subject.getType().equals(SubjectType.PLAYER)) {
			return checkOfflinePlayerPermission(subject.getUUID(), permission, opPerms);
		} else if (subject.getType().equals(SubjectType.IP)) {
			return center.resolver().getPlayers(subject.getIP()).stream().anyMatch((uuid) -> checkOfflinePlayerPermission(uuid, permission, opPerms));
		}
		throw new InvalidSubjectException("Subject type is completely missing!");
	}
	
	private Stream<? extends Player> applicable(Predicate<? super Player> predicate) {
		return plugin.getServer().getOnlinePlayers().stream().filter(predicate);
	}
	
	Stream<? extends Player> applicable(Subject subject) {
		switch (subject.getType()) {
		case PLAYER:
			return applicable((player) -> subject.getUUID().equals(player.getUniqueId()));
		case IP:
			return applicable((player) -> center.resolver().hasIp(player.getUniqueId(), subject.getIP()));
		default:
			return Stream.empty();
		}
	}
	
	@Override
	public UUID uuidFromName(String name) {
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			if (player.getName().equalsIgnoreCase(name)) {
				return player.getUniqueId();
			}
		}
		for (OfflinePlayer player : plugin.getServer().getOfflinePlayers()) {
			if (player.getName().equalsIgnoreCase(name)) {
				return player.getUniqueId();
			}
		}
		return null;
	}
	
	@Override
	public String nameFromUUID(UUID uuid) {
		OfflinePlayer player = plugin.getServer().getOfflinePlayer(uuid);
		return player != null ? player.getName() : null;
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
