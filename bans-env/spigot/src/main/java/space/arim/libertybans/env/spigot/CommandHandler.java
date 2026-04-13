/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.env.spigot;

import jakarta.inject.Inject;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.LoggerFactory;
import space.arim.api.env.AudienceRepresenter;
import space.arim.api.env.bukkit.BukkitCommandSkeleton;
import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.Interlocutor;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.omnibus.util.ArraysUtil;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class CommandHandler extends BukkitCommandSkeleton implements PlatformListener, PluginIdentifiableCommand {

	private final CommandHelper commandHelper;
	private final @Nullable String aliasTarget;
	
	CommandHandler(CommandHelper commandHelper, String command, @Nullable String aliasTarget) {
		super(command);
		this.commandHelper = commandHelper;
        this.aliasTarget = aliasTarget;
	}

	public static class CommandHelper {
		
		private final InternalFormatter formatter;
		private final Interlocutor interlocutor;
		private final AudienceRepresenter<CommandSender> audienceRepresenter;
		private final Commands commands;
		private final Plugin plugin;
		private final CommandMapHelper commandMapHelper;

		@Inject
		public CommandHelper(InternalFormatter formatter, Interlocutor interlocutor,
							 AudienceRepresenter<CommandSender> audienceRepresenter, Commands commands,
							 Plugin plugin, CommandMapHelper commandMapHelper) {
			this.formatter = formatter;
			this.interlocutor = interlocutor;
			this.audienceRepresenter = audienceRepresenter;
			this.commands = commands;
			this.plugin = plugin;
			this.commandMapHelper = commandMapHelper;
		}

		private CmdSender adaptSender(CommandSender platformSender) {
			if (platformSender instanceof Player) {
				return new SpigotCmdSender.PlayerSender(formatter, interlocutor, audienceRepresenter,
						(Player) platformSender, plugin);
			}
			return new SpigotCmdSender.ConsoleSender(formatter, interlocutor, audienceRepresenter,
					platformSender, plugin);
		}
	}

	@Override
	public Plugin getPlugin() {
		return commandHelper.plugin;
	}

	@Override
	public void register() {
		CommandMapHelper commandMapHelper = commandHelper.commandMapHelper;
		CommandMap commandMap = commandMapHelper.getCommandMap();
		if (commandMapHelper.getKnownCommands(commandMap) == null && aliasTarget != null) {
			return;
		}
		commandMap.register(getName(), commandHelper.plugin.getName().toLowerCase(Locale.ENGLISH), this);

		Command otherCommand = commandMap.getCommand(getName());
		boolean belongsToUs = otherCommand instanceof PluginIdentifiableCommand otherPluginCommand
				&& otherPluginCommand.getPlugin() == getPlugin();
		if (!belongsToUs && !getName().equals(Commands.BASE_COMMAND_NAME)) {

			String belongingTo;
			if (otherCommand instanceof PluginIdentifiableCommand otherPluginCommand) {
				Plugin otherPlugin = otherPluginCommand.getPlugin();
				belongingTo = " belonging to plugin " + otherPlugin.getDescription().getFullName();
			} else {
				belongingTo = "";
			}
			LoggerFactory.getLogger(getClass()).warn(
					"""
							LibertyBans attempted to register '/{}', but it already exists as {}{}.
							
							If you want LibertyBans to control this command, you must solve the command registration
							conflict with the other plugin:
							1. First check if the other plugin has an option to disable the command. If it does, use it.
							   Good plugins will provide this option, but many, including Essentials, do not.
							2. Otherwise, you will have to use the server's commands.yml to specify command overrides.
							   You can find information about this at https://bukkit.fandom.com/wiki/Commands.yml
							3. It is also possible to use an alias plugin to specify which plugin uses the command.
							   Many alias plugins exist on popular plugin release websites.

							If you do not want LibertyBans to control this command, you should disable it in the
							alias configuration.
							""",
					getName(), otherCommand, belongingTo
			);
		}
	}

	@Override
	public void unregister() {
		CommandMapHelper commandMapHelper = commandHelper.commandMapHelper;
		CommandMap commandMap = commandMapHelper.getCommandMap();
		Map<String, Command> knownCommands = commandMapHelper.getKnownCommands(commandMap);
		if (knownCommands == null) {
			return;
		}
		while (knownCommands.values().remove(this)) {
			// Remove from map
		}
	}

	private String[] adaptArgs(String[] args) {
		if (aliasTarget != null) {
			return ArraysUtil.expandAndInsert(args, aliasTarget, 0);
		}
		return args;
	}

	@Override
	protected void execute(CommandSender platformSender, String[] args) {
		commandHelper.commands.execute(
				commandHelper.adaptSender(platformSender),
				ArrayCommandPackage.create(adaptArgs(args))
		);
	}

	@Override
	protected List<String> suggest(CommandSender platformSender, String[] args) {
		return commandHelper.commands.suggest(
				commandHelper.adaptSender(platformSender),
				adaptArgs(args)
		);
	}

	@Override
	public boolean testPermissionSilent(CommandSender platformSender) {
		return commandHelper.commands.hasPermissionFor(
				commandHelper.adaptSender(platformSender),
				Objects.requireNonNullElse(aliasTarget, getName())
		);
	}

}
