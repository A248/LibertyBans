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

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.OmnibusProvider;
import space.arim.omnibus.util.ThisClass;

import space.arim.uuidvault.api.UUIDVault;
import space.arim.uuidvault.plugin.UUIDVaultSpigot;

import space.arim.api.env.BukkitPlatformHandle;
import space.arim.api.env.PlatformHandle;

import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.env.AbstractEnv;
import space.arim.libertybans.core.env.PlatformListener;

import space.arim.morepaperlib.MorePaperLib;

import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotEnv extends AbstractEnv {

	final LibertyBansCore core;
	final BukkitPlatformHandle handle;
	
	private final CommandMap commandMap;
	private final Field knownCommandsField;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	private final SpigotEnforcer enforcer;
	
	public SpigotEnv(JavaPlugin plugin, Path folder) {
		core = new LibertyBansCore(OmnibusProvider.getOmnibus(), folder, this);
		handle = new BukkitPlatformHandle(plugin);

		enforcer = new SpigotEnforcer(this);

		commandMap = new MorePaperLib(plugin).getServerCommandMap();
		knownCommandsField = getKnownCommandsField(commandMap);
	}
	
	private static Field getKnownCommandsField(CommandMap commandMap) {
		if (commandMap instanceof SimpleCommandMap) {
			Field knownCommandsField;
			try {
				knownCommandsField = SimpleCommandMap.class.getClass().getField("knownCommands");
			} catch (NoSuchFieldException | SecurityException ex) {
				logger.warn(
						"Unable to find your server's CommandMap's 'knownCommands' field. "
						+ CommandHandler.COMMAND_MAP_WARNING);
				knownCommandsField = null;
			}
			return knownCommandsField;
		} else {
			Class<?> replacementClass = commandMap.getClass();
			String pluginName = "Unknown";
			try {
				JavaPlugin.getProvidingPlugin(replacementClass);
			} catch (IllegalArgumentException ignored) {}
			logger.warn(
					"Your server's CommandMap is not an instance of SimpleCommandMap. Rather, it is {} from plugin {}. "
					+ "This could be disastrous and you should remove the offending plugin or speak to its author(s), "
					+ "as many plugins assume SimpleCommandMap as the norms. "
					+ CommandHandler.COMMAND_MAP_WARNING,
					replacementClass, pluginName);
			return null;
		}
	}
	
	JavaPlugin getPlugin() {
		return handle.getPlugin();
	}
	
	CommandMap getCommandMap() {
		return commandMap;
	}
	
	Field getCommandMapKnownCommandsField() {
		return knownCommandsField;
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
		if (UUIDVault.get() == null) {
			new UUIDVaultSpigot(getPlugin()) {
				@Override
				protected boolean setInstancePassive() {
					return super.setInstancePassive();
				}
			}.setInstancePassive();
		}
		core.startup();
	}
	
	@Override
	protected void restart0() {
		core.restart();
	}

	@Override
	protected void shutdown0() {
		core.shutdown();
	}

	@Override
	protected void infoMessage(String message) {
		logger.info(message);
	}
	
	boolean hasPermissionSafe(Player player, String permission) {
		// TODO: Account for non-thread safe permission plugins
		return player.hasPermission(permission);
	}

	@Override
	public Set<PlatformListener> createListeners() {
		return Set.of(
				new ConnectionListener(this),
				new ChatListener(this),
				new CommandHandler(this, Commands.BASE_COMMAND_NAME, false));
	}

	@Override
	public PlatformListener createAliasCommand(String command) {
		return new CommandHandler(this, command, true);
	}
	
}
