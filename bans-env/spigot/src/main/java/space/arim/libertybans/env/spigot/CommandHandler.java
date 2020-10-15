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
import java.lang.reflect.InaccessibleObjectException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ArraysUtil;
import space.arim.omnibus.util.ThisClass;

import space.arim.api.env.util.command.BukkitCommandSkeleton;

import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.PlatformListener;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

class CommandHandler extends BukkitCommandSkeleton implements PlatformListener {

	private final SpigotEnv env;
	private final boolean alias;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	private static final String COMMAND_MAP_WARNING = 
			"LibertyBans has limited support for this situation - the plugin will continue to work, but restarting it may "
			+ "encounter weird issues with command registration, as well as potential memory leaks.";
	
	CommandHandler(SpigotEnv env, String command, boolean alias) {
		super(command);
		this.env = env;
		this.alias = alias;
	}
	
	static Field getKnownCommandsField(CommandMap commandMap) {
		if (!(commandMap instanceof SimpleCommandMap)) {
			/*
			 * CommandMap was replaced by a criminal plugin
			 */
			Class<?> replacementClass = commandMap.getClass();
			String pluginName = "Unknown";
			try {
				pluginName = JavaPlugin.getProvidingPlugin(replacementClass).getDescription().getFullName();
			} catch (IllegalArgumentException ignored) {}
			logger.warn(
					"Your server's CommandMap is not an instance of SimpleCommandMap. Rather, it is {} from plugin {}. "
					+ "This could be disastrous and you should remove the offending plugin or speak to its author(s), "
					+ "as many plugins assume SimpleCommandMap as the norm. "
					+ COMMAND_MAP_WARNING,
					replacementClass, pluginName);
			return null;
		}
		Field knownCommandsField;
		try {
			knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");

		} catch (NoSuchFieldException | SecurityException ex) {
			logger.warn(
					"Unable to find your server's CommandMap's 'knownCommands' field. "
					+ COMMAND_MAP_WARNING, ex);
			knownCommandsField = null;
		}
		return knownCommandsField;
	}
	
	@Override
	public void register() {
		env.getCommandMap().register(getName(), env.getPlugin().getName().toLowerCase(Locale.ENGLISH), this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void unregister() {
		CommandMap commandMap = env.getCommandMap();
		Field knownCommandsField = env.getCommandMapKnownCommandsField();
		if (knownCommandsField == null) {
			logger.debug("Skipping command unregistration as 'knownCommands' field was not previously found");

		} else {
			Map<String, Command> knownCommands = null;
			try {
				knownCommandsField.setAccessible(true);
				knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
			} catch (ClassCastException | IllegalArgumentException | IllegalAccessException | SecurityException
					| InaccessibleObjectException ex) {
				logger.warn("Unable to retrieve server command map's known commands. " + COMMAND_MAP_WARNING, ex);
			}
			if (knownCommands != null) {
				do {
					logger.trace("Iteration: removing command from map");
				} while (knownCommands.values().remove(this));
			}
		}
	}
	
	private CmdSender adaptSender(CommandSender platformSender) {
		return (platformSender instanceof Player) ?
				new PlayerCmdSender(env, (Player) platformSender)
				: new ConsoleCmdSender(env, platformSender);
	}
	
	private String[] adaptArgs(String[] args) {
		if (alias) {
			return ArraysUtil.expandAndInsert(args, getName(), 0);
		}
		return args;
	}

	@Override
	protected void execute(CommandSender platformSender, String[] args) {
		env.core.getCommands().execute(adaptSender(platformSender), new ArrayCommandPackage(getName(), adaptArgs(args)));
	}

	@Override
	protected List<String> suggest(CommandSender platformSender, String[] args) {
		return env.core.getCommands().suggest(adaptSender(platformSender), adaptArgs(args));
	}

}
