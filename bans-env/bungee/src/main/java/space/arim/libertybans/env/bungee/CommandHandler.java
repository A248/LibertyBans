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

package space.arim.libertybans.env.bungee;

import jakarta.inject.Inject;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.api.env.AudienceRepresenter;
import space.arim.libertybans.core.commands.CommandSource;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AliasCommand;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.Interlocutor;

import java.util.Map;
import java.util.Objects;

public final class CommandHandler extends Command implements TabExecutor, AliasCommand {

	private final CommandHelper commandHelper;
	private final @Nullable String aliasTarget;
	
	CommandHandler(CommandHelper commandHelper, String command, @Nullable String aliasTarget) {
		super(command);
		this.commandHelper = commandHelper;
		this.aliasTarget = aliasTarget;
	}

	public static final class CommandHelper {

		private final InternalFormatter formatter;
		private final Interlocutor interlocutor;
		private final AudienceRepresenter<CommandSender> audienceRepresenter;
		private final Commands commands;
		private final Plugin plugin;

		@Inject
		public CommandHelper(InternalFormatter formatter, Interlocutor interlocutor,
							 AudienceRepresenter<CommandSender> audienceRepresenter,
							 Commands commands, Plugin plugin) {
			this.formatter = formatter;
			this.interlocutor = interlocutor;
			this.audienceRepresenter = audienceRepresenter;
			this.commands = commands;
			this.plugin = plugin;
		}

		private CmdSender adaptSender(CommandSender platformSender) {
			if (platformSender instanceof ProxiedPlayer player) {
				return new BungeeCmdSender.PlayerSender(
						formatter, interlocutor, audienceRepresenter, player, plugin);
			}
			return new BungeeCmdSender.ConsoleSender(
					formatter, interlocutor, audienceRepresenter, platformSender, plugin);
		}
	}

	@Override
	public void register(RegisterOutcome outcome) {
		Plugin plugin = commandHelper.plugin;
		PluginManager pluginManager = plugin.getProxy().getPluginManager();
		pluginManager.registerCommand(plugin, this);
		for (Map.Entry<String, Command> entry : pluginManager.getCommands()) {
			if (entry.getKey().equalsIgnoreCase(getName())) {
				if (entry.getValue() == this) {
					outcome.success();
				} else {
					outcome.alreadyRegistered(entry.getValue(), null);
				}
				return;
			}
		}
		outcome.disappear();
	}

	@Override
	public void unregister() {
		commandHelper.plugin.getProxy().getPluginManager().unregisterCommand(this);
	}

	private CommandSource adaptArgs(String[] argArray) {
		CommandSource args = new CommandSource.OfArray(argArray);
		if (aliasTarget != null) {
			args = new CommandSource.Prepended(aliasTarget, args);
		}
		return args;
	}

	@Override
	public void execute(CommandSender platformSender, String[] args) {
		commandHelper.commands.execute(commandHelper.adaptSender(platformSender), adaptArgs(args));
	}

	@Override
	public Iterable<String> onTabComplete(CommandSender platformSender, String[] args) {
		return commandHelper.commands.suggest(commandHelper.adaptSender(platformSender), adaptArgs(args));
	}

	@Override
	public boolean hasPermission(CommandSender platformSender) {
		return commandHelper.commands.hasPermissionFor(
				commandHelper.adaptSender(platformSender),
				Objects.requireNonNullElse(aliasTarget, getName())
		);
	}
}
