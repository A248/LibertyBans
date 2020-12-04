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

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ArraysUtil;
import space.arim.omnibus.util.ThisClass;

import space.arim.api.env.util.command.BukkitCommandSkeleton;

import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.PlatformListener;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandHandler extends BukkitCommandSkeleton implements PlatformListener {

	private final DependencyPackage dependencies;
	private final boolean alias;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	CommandHandler(DependencyPackage dependencies, String command, boolean alias) {
		super(command);
		this.dependencies = dependencies;
		this.alias = alias;
	}
	
	public static class DependencyPackage {
		
		final SpigotCmdSender.CmdSenderDependencies parentDependencies;
		final Commands commands;
		final JavaPlugin plugin;
		final CommandMapHelper commandMapHelper;
		
		@Inject
		public DependencyPackage(SpigotCmdSender.CmdSenderDependencies parentDependencies, Commands commands,
				JavaPlugin plugin, CommandMapHelper commandMapHelper) {
			this.parentDependencies = parentDependencies;
			this.commands = commands;
			this.plugin = plugin;
			this.commandMapHelper = commandMapHelper;
		}
		
	}
	
	@Override
	public void register() {
		CommandMapHelper commandMapHelper = dependencies.commandMapHelper;
		CommandMap commandMap = commandMapHelper.getCommandMap();
		if (commandMap == null
				|| commandMapHelper.getKnownCommandsField(commandMap) == null && alias) {
			return;
		}
		commandMap.register(getName(), dependencies.plugin.getName().toLowerCase(Locale.ENGLISH), this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void unregister() {
		CommandMapHelper commandMapHelper = dependencies.commandMapHelper;
		CommandMap commandMap = commandMapHelper.getCommandMap();
		if (commandMap == null) {
			return;
		}
		Field knownCommandsField = commandMapHelper.getKnownCommandsField(commandMap);
		if (knownCommandsField == null) {
			if (!alias) {
				logger.warn("As stated previously, /libertybans cannot be unregistered.");
			}
			return;
		}
		Map<String, Command> knownCommands;
		try {
			knownCommandsField.setAccessible(true);
			knownCommands = (Map<String, Command>) knownCommandsField.get(commandMap);
		} catch (ClassCastException | IllegalArgumentException | IllegalAccessException | SecurityException
				| InaccessibleObjectException ex) {
			logger.warn("Unable to retrieve server command map's known commands.", ex);
			return;
		}
		while (knownCommands.values().remove(this)) {
			// Remove from map
		}
	}

	private CmdSender adaptSender(CommandSender platformSender) {
		SpigotCmdSender.CmdSenderDependencies parentDependencies = dependencies.parentDependencies;
		return (platformSender instanceof Player) ?
				new SpigotCmdSender.PlayerSender(parentDependencies, (Player) platformSender)
				: new SpigotCmdSender.ConsoleSender(parentDependencies, platformSender);
	}

	private String[] adaptArgs(String[] args) {
		if (alias) {
			return ArraysUtil.expandAndInsert(args, getName(), 0);
		}
		return args;
	}

	@Override
	protected void execute(CommandSender platformSender, String[] args) {
		dependencies.commands.execute(adaptSender(platformSender), new ArrayCommandPackage(getName(), adaptArgs(args)));
	}

	@Override
	protected List<String> suggest(CommandSender platformSender, String[] args) {
		return dependencies.commands.suggest(adaptSender(platformSender), adaptArgs(args));
	}

}
