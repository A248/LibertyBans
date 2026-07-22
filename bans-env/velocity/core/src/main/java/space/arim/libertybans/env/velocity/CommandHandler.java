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

package space.arim.libertybans.env.velocity;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.libertybans.core.commands.CommandSource;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AliasCommand;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.Interlocutor;

import java.util.List;
import java.util.Objects;

public final class CommandHandler implements SimpleCommand, AliasCommand {

	private final CommandHelper commandHelper;
	private final String name;
	private final @Nullable String aliasTarget;

	CommandHandler(CommandHelper commandHelper, String name, @Nullable String aliasTarget) {
		this.commandHelper = commandHelper;
		this.name = name;
		this.aliasTarget = aliasTarget;
	}

	public static class CommandHelper {

		private final InternalFormatter formatter;
		private final Interlocutor interlocutor;
		private final Commands commands;
		private final PluginContainer plugin;
		private final ProxyServer server;

		@Inject
		public CommandHelper(InternalFormatter formatter, Interlocutor interlocutor,
							 Commands commands, PluginContainer plugin, ProxyServer server) {
			this.formatter = formatter;
			this.interlocutor = interlocutor;
			this.commands = commands;
			this.plugin = plugin;
			this.server = server;
		}

		private CmdSender adaptSender(com.velocitypowered.api.command.CommandSource platformSender) {
			if (platformSender instanceof Player player) {
				return new VelocityCmdSender.PlayerSender(formatter, interlocutor, player, server);
			}
			return new VelocityCmdSender.ConsoleSender(formatter, interlocutor, platformSender, server);
		}

	}

	@Override
	public void register(AliasCommand.RegisterOutcome registerOutcome) {
		CommandManager commandManager = commandHelper.server.getCommandManager();
		CommandMeta commandMeta = commandManager
				.metaBuilder(name)
				.plugin(commandHelper.plugin)
				.build();
		commandManager.register(commandMeta, this);
		CommandMeta actualMeta = commandManager.getCommandMeta(name);
		Object registeringPlugin;
		if (actualMeta == null) {
			registerOutcome.disappear();
		} else if ((registeringPlugin = actualMeta.getPlugin()) == commandHelper.plugin) {
			registerOutcome.success();
		} else {
			String belongingTo;
			if (registeringPlugin instanceof PluginContainer otherPlugin) {
				PluginDescription description = otherPlugin.getDescription();
				belongingTo = description.getName().orElse(description.getId()) + ' ' + description.getVersion().orElse("0.0");
			} else if (registeringPlugin != null) {
				belongingTo = registeringPlugin.toString();
			} else {
				belongingTo = null;
			}
			registerOutcome.alreadyRegistered(actualMeta, belongingTo);
		}
	}

	@Override
	public void unregister() {
		CommandManager cmdManager = commandHelper.server.getCommandManager();
		cmdManager.unregister(name);
	}

	private CommandSource adaptArgs(Invocation invocation, boolean tabComplete) {
		String[] args = invocation.arguments();
		if (aliasTarget != null) {
			if (tabComplete && args.length == 0) {
				// This fixes tab completion for aliased commands
				// Tab completion relies on the existence of empty strings
				return new CommandSource.OfArray(aliasTarget, "");
			}
			return new CommandSource.Prepended(aliasTarget, new CommandSource.OfArray(args));
		}
		return new CommandSource.OfArray(args);
	}

	@Override
	public void execute(Invocation invocation) {
		commandHelper.commands.execute(
				commandHelper.adaptSender(invocation.source()), adaptArgs(invocation, false)
		);
	}

	@Override
	public List<String> suggest(Invocation invocation) {
		return commandHelper.commands.suggest(
				commandHelper.adaptSender(invocation.source()), adaptArgs(invocation, true)
		);
	}

	@Override
	public boolean hasPermission(Invocation invocation) {
		return commandHelper.commands.hasPermissionFor(
				commandHelper.adaptSender(invocation.source()),
				Objects.requireNonNullElse(aliasTarget, name)
		);
	}

}
