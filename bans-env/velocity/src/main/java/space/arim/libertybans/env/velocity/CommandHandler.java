/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import jakarta.inject.Inject;
import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.Interlocutor;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.omnibus.util.ArraysUtil;

import java.util.List;

public final class CommandHandler implements SimpleCommand, PlatformListener {

	private final CommandHelper commandHelper;
	private final String name;
	private final boolean alias;

	CommandHandler(CommandHelper commandHelper, String name, boolean alias) {
		this.commandHelper = commandHelper;
		this.name = name;
		this.alias = alias;
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

		private CmdSender adaptSender(CommandSource platformSender) {
			if (platformSender instanceof Player player) {
				return new VelocityCmdSender.PlayerSender(formatter, interlocutor, player, server);
			}
			return new VelocityCmdSender.ConsoleSender(formatter, interlocutor, platformSender, server);
		}

	}

	@Override
	public void register() {
		CommandManager commandManager = commandHelper.server.getCommandManager();
		CommandMeta commandMeta = commandManager
				.metaBuilder(name)
				.plugin(commandHelper.plugin)
				.build();
		commandManager.register(commandMeta, this);
	}

	@Override
	public void unregister() {
		CommandManager cmdManager = commandHelper.server.getCommandManager();
		cmdManager.unregister(name);
	}

	private String[] adaptArgs(String[] args, boolean tabComplete) {
		if (alias) {
			if (tabComplete && args.length == 0) {
				// This fixes tab completion for aliased commands
				// Tab completion relies on the existence of empty strings
				return new String[] {name, ""};
			}
			return ArraysUtil.expandAndInsert(args, name, 0);
		}
		return args;
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource platformSender = invocation.source();
		String[] args = invocation.arguments();
		commandHelper.commands.execute(
				commandHelper.adaptSender(platformSender),
				ArrayCommandPackage.create(adaptArgs(args, false))
		);
	}

	@Override
	public List<String> suggest(Invocation invocation) {
		CommandSource platformSender = invocation.source();
		String[] args = invocation.arguments();
		return commandHelper.commands.suggest(
				commandHelper.adaptSender(platformSender),
				adaptArgs(args, true)
		);
	}

	@Override
	public boolean hasPermission(Invocation invocation) {
		CommandSource platformSender = invocation.source();
		return commandHelper.commands.hasPermissionFor(
				commandHelper.adaptSender(platformSender),
				name
		);
	}

}
