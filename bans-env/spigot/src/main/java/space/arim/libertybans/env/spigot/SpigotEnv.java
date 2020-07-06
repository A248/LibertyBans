/* 
 * LibertyBans-env-spigot
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-spigot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-spigot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-spigot. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.spigot;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.chat.BaseComponent;

import space.arim.universal.util.concurrent.CentralisedFuture;

import space.arim.uuidvault.api.UUIDVault;
import space.arim.uuidvault.plugin.spigot.UUIDVaultSpigot;

import space.arim.api.chat.MessageParserUtil;
import space.arim.api.env.BungeeComponentParser;
import space.arim.api.env.convention.SpigotPlatformConvention;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.env.AbstractEnv;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.OnlineTarget;

public class SpigotEnv extends AbstractEnv {

	final JavaPlugin plugin;
	final LibertyBansCore core;
	
	private final ConnectionListener listener;
	
	public SpigotEnv(JavaPlugin plugin, File folder) {
		this.plugin = plugin;
		core = new LibertyBansCore(new SpigotPlatformConvention(plugin).getRegistry(), folder, this);
		listener = new ConnectionListener(this);
		if (UUIDVault.get() == null) {
			new UUIDVaultSpigot(plugin).setInstancePassive();
		}
	}
	
	public SpigotEnv(JavaPlugin plugin) {
		this(plugin, plugin.getDataFolder());
	}

	@Override
	public void sendToThoseWithPermission(String permission, String message) {
		Consumer<Player> forEach;
		if (core.getFormatter().isUseJson()) {
			BaseComponent[] parsed = new BungeeComponentParser().parseJson(message);
			forEach = (player) -> player.spigot().sendMessage(parsed);
		} else {
			forEach = (player) -> player.sendMessage(message);
		}
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			if (player.hasPermission(permission)) {
				forEach.accept(player);
			}
		}
	}
	
	void sendJson(CommandSender sender, String jsonable) {
		BungeeComponentParser parser = new BungeeComponentParser();
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (core.getFormatter().isUseJson()) {
				player.spigot().sendMessage(parser.parseJson(jsonable));
			} else {
				player.sendMessage(jsonable);
			}
		} else {
			sender.sendMessage(new MessageParserUtil().removeRawJson(jsonable));
		}
	}

	@Override
	protected void startup0() {
		core.startup();
		plugin.getCommand("libertybans").setExecutor((sender, cmd, label, args) -> {
			CmdSender iSender;
			if (sender instanceof Player) {
				iSender = new PlayerCmdSender(this, (Player) sender);
			} else {
				iSender = new ConsoleCmdSender(this, sender);
			}
			core.getCommands().execute(iSender, new ArrayCommandPackage(cmd.getName(), args));
			return true;
		});
		plugin.getServer().getPluginManager().registerEvents(listener, plugin);
	}

	@Override
	protected void shutdown0() {
		plugin.getCommand("libertybans").setExecutor(plugin);
		HandlerList.unregisterAll(listener);
		core.shutdown();
	}

	@Override
	protected void infoMessage(String message) {
		plugin.getLogger().info(message);
	}

	@Override
	public void kickByUUID(UUID uuid, String message) {
		core.getFuturesFactory().executeSync(() -> {
			Player player = plugin.getServer().getPlayer(uuid);
			if (player != null) {
				player.kickPlayer(ChatColor.translateAlternateColorCodes('&', message));
			}
		});
	}
	
	boolean hasPermissionSafe(Player player, String permission) {
		// TODO: Account for non-thread safe permission plugins
		return player.hasPermission(permission);
	}

	@Override
	public CentralisedFuture<Set<OnlineTarget>> getOnlineTargets() {
		return core.getFuturesFactory().supplySync(() -> {
			Set<OnlineTarget> result = new HashSet<>();
			for (Player player : plugin.getServer().getOnlinePlayers()) {
				result.add(new SpigotOnlineTarget(this, player));
			}
			return result;
		});
	}
	
}
