/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import space.arim.api.env.AudienceRepresenter;
import space.arim.api.env.bukkit.BukkitCommandSkeleton;
import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.omnibus.util.ArraysUtil;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CommandHandler extends BukkitCommandSkeleton implements PlatformListener {

	private final CommandHelper commandHelper;
	private final boolean alias;
	
	CommandHandler(CommandHelper commandHelper, String command, boolean alias) {
		super(command);
		this.commandHelper = commandHelper;
		this.alias = alias;
	}
	
	public static class CommandHelper {
		
		private final InternalFormatter formatter;
		private final AudienceRepresenter<CommandSender> audienceRepresenter;
		private final Commands commands;
		final Plugin plugin;
		private final FactoryOfTheFuture futuresFactory;
		final CommandMapHelper commandMapHelper;
		
		@Inject
		public CommandHelper(InternalFormatter formatter, AudienceRepresenter<CommandSender> audienceRepresenter,
							 Commands commands, Plugin plugin,
							 FactoryOfTheFuture futuresFactory, CommandMapHelper commandMapHelper) {
			this.formatter = formatter;
			this.audienceRepresenter = audienceRepresenter;
			this.commands = commands;
			this.plugin = plugin;
			this.futuresFactory = futuresFactory;
			this.commandMapHelper = commandMapHelper;
		}

		private CmdSender adaptSender(CommandSender platformSender) {
			if (platformSender instanceof Player) {
				return new SpigotCmdSender.PlayerSender(formatter, audienceRepresenter,
						(Player) platformSender, plugin, futuresFactory);
			}
			return new SpigotCmdSender.ConsoleSender(formatter, audienceRepresenter,
					platformSender, plugin, futuresFactory);
		}

		void execute(CommandSender platformSender, CommandPackage command) {
			commands.execute(adaptSender(platformSender), command);
		}

		List<String> suggest(CommandSender platformSender, String[] args) {
			return commands.suggest(adaptSender(platformSender), args);
		}

		boolean testPermission(CommandSender platformSender, String command) {
			return commands.hasPermissionFor(adaptSender(platformSender), command);
		}

	}
	
	@Override
	public void register() {
		CommandMapHelper commandMapHelper = commandHelper.commandMapHelper;
		CommandMap commandMap = commandMapHelper.getCommandMap();
		if (commandMapHelper.getKnownCommands(commandMap) == null && alias) {
			return;
		}
		commandMap.register(getName(), commandHelper.plugin.getName().toLowerCase(Locale.ENGLISH), this);
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
		if (alias) {
			return ArraysUtil.expandAndInsert(args, getName(), 0);
		}
		return args;
	}

	@Override
	protected void execute(CommandSender platformSender, String[] args) {
		commandHelper.execute(platformSender, ArrayCommandPackage.create(adaptArgs(args)));
	}

	@Override
	protected List<String> suggest(CommandSender platformSender, String[] args) {
		return commandHelper.suggest(platformSender, adaptArgs(args));
	}

	@Override
	public boolean testPermissionSilent(CommandSender platformSender) {
		return commandHelper.testPermission(platformSender, getName());
	}

}
