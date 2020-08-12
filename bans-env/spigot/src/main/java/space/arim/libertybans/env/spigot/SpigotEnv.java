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

import space.arim.omnibus.OmnibusProvider;

import space.arim.api.env.BukkitPlatformHandle;
import space.arim.api.env.PlatformHandle;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.env.AbstractEnv;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotEnv extends AbstractEnv {

	final LibertyBansCore core;
	final BukkitPlatformHandle handle;
	
	private final ConnectionListener joinListener;
	private final SpigotCommands commands;
	
	private final SpigotEnforcer enforcer;
	
	public SpigotEnv(JavaPlugin plugin, Path folder) {
		core = new LibertyBansCore(OmnibusProvider.getOmnibus(), folder, this);
		handle = new BukkitPlatformHandle(plugin);

		commands = new SpigotCommands(this);
		joinListener = new ConnectionListener(this);

		enforcer = new SpigotEnforcer(this);
	}
	
	JavaPlugin getPlugin() {
		return handle.getPlugin();
	}

	@Override
	public PlatformHandle getPlatformHandle() {
		return handle;
	}
	
	@Override
	public SpigotEnforcer getEnforcer() {
		return enforcer;
	}

	@Override
	protected void startup0() {
		core.startup();
		JavaPlugin plugin = getPlugin();
		plugin.getServer().getPluginManager().registerEvents(joinListener, plugin);
		plugin.getCommand(Commands.BASE_COMMAND_NAME).setExecutor(commands);
	}
	
	@Override
	protected void restart0() {
		core.restart();
	}

	@Override
	protected void shutdown0() {
		JavaPlugin plugin = getPlugin();
		plugin.getCommand(Commands.BASE_COMMAND_NAME).setExecutor(plugin);
		HandlerList.unregisterAll(joinListener);
		core.shutdown();
	}

	@Override
	protected void infoMessage(String message) {
		getPlugin().getLogger().info(message);
	}
	
	boolean hasPermissionSafe(Player player, String permission) {
		// TODO: Account for non-thread safe permission plugins
		return player.hasPermission(permission);
	}
	
}
