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

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import space.arim.omnibus.OmnibusProvider;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.BukkitPlatformHandle;
import space.arim.api.env.PlatformHandle;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.env.AbstractEnv;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.OnlineTarget;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotEnv extends AbstractEnv {

	final LibertyBansCore core;
	final BukkitPlatformHandle handle;
	
	private final ConnectionListener listener;
	
	public SpigotEnv(JavaPlugin plugin, Path folder) {
		core = new LibertyBansCore(OmnibusProvider.getOmnibus(), folder, this);
		listener = new ConnectionListener(this);
		handle = new BukkitPlatformHandle(plugin);
	}
	
	public SpigotEnv(JavaPlugin plugin) {
		this(plugin, plugin.getDataFolder().toPath());
	}
	
	JavaPlugin getPlugin() {
		return handle.getPlugin();
	}
	
	@Override
	public Class<?> getPluginClass() {
		return getPlugin().getClass();
	}

	@Override
	public PlatformHandle getPlatformHandle() {
		return handle;
	}

	@Override
	public void sendToThoseWithPermission(String permission, SendableMessage message) {
		for (Player player : getPlugin().getServer().getOnlinePlayers()) {
			if (player.hasPermission(permission)) {
				handle.sendMessage(player, message);
			}
		}
	}

	@Override
	protected void startup0() {
		core.startup();
		JavaPlugin plugin = getPlugin();
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
	protected void restart0() {
		core.restart();
	}

	@Override
	protected void shutdown0() {
		JavaPlugin plugin = getPlugin();
		plugin.getCommand("libertybans").setExecutor(plugin);
		HandlerList.unregisterAll(listener);
		core.shutdown();
	}

	@Override
	protected void infoMessage(String message) {
		getPlugin().getLogger().info(message);
	}

	@Override
	public void kickByUUID(UUID uuid, SendableMessage message) {
		core.getFuturesFactory().executeSync(() -> {
			Player player = getPlugin().getServer().getPlayer(uuid);
			if (player != null) {
				handle.disconnectUser(player, message);
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
			for (Player player : getPlugin().getServer().getOnlinePlayers()) {
				result.add(new SpigotOnlineTarget(this, player));
			}
			return result;
		});
	}
	
}
