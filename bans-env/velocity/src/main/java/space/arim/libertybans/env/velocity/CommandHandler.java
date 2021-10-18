/* 
 * LibertyBans-env-velocity
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-velocity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-velocity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-velocity. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.velocity;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import jakarta.inject.Inject;
import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.omnibus.util.ArraysUtil;

import java.util.List;

public class CommandHandler implements SimpleCommand, PlatformListener {

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
		private final Commands commands;
		final ProxyServer server;
		
		@Inject
		public CommandHelper(InternalFormatter formatter, Commands commands,
							 ProxyServer server) {
			this.formatter = formatter;
			this.commands = commands;
			this.server = server;
		}

		private CmdSender adaptSender(CommandSource platformSender) {
			if (platformSender instanceof Player) {
				return new VelocityCmdSender.PlayerSender(formatter, (Player) platformSender, server);
			}
			return new VelocityCmdSender.ConsoleSender(formatter, platformSender, server);
		}

		void execute(CommandSource platformSender, CommandPackage command) {
			commands.execute(adaptSender(platformSender), command);
		}

		List<String> suggest(CommandSource platformSender, String[] args) {
			return commands.suggest(adaptSender(platformSender), args);
		}
	}
	
	@Override
	public void register() {
		CommandManager cmdManager = commandHelper.server.getCommandManager();
		cmdManager.register(cmdManager.metaBuilder(name).build(), this);
	}

	@Override
	public void unregister() {
		CommandManager cmdManager = commandHelper.server.getCommandManager();
		cmdManager.unregister(name);
	}
	
	private String[] adaptArgs(String[] args) {
		if (alias) {
			return ArraysUtil.expandAndInsert(args, name, 0);
		}
		return args;
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource platformSender = invocation.source();
		String[] args = invocation.arguments();
		commandHelper.execute(platformSender, ArrayCommandPackage.create(adaptArgs(args)));
	}
	
	@Override
	public List<String> suggest(Invocation invocation) {
		CommandSource platformSender = invocation.source();
		String[] args = invocation.arguments();
		return commandHelper.suggest(platformSender, adaptArgs(args));
	}
	
}
