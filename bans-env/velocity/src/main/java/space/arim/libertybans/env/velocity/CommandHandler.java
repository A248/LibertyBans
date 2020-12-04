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

import java.util.List;

import jakarta.inject.Inject;

import space.arim.omnibus.util.ArraysUtil;

import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.PlatformListener;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

public class CommandHandler implements SimpleCommand, PlatformListener {

	private final DependencyPackage dependencies;
	private final String name;
	private final boolean alias;
	
	CommandHandler(DependencyPackage dependencies, String name, boolean alias) {
		this.dependencies = dependencies;
		this.name = name;
		this.alias = alias;
	}
	
	public static class DependencyPackage {
		
		final VelocityCmdSender.CmdSenderDependencies parentDependencies;
		final Commands commands;
		final ProxyServer server;
		
		@Inject
		public DependencyPackage(VelocityCmdSender.CmdSenderDependencies parentDependencies, Commands commands,
				ProxyServer server) {
			this.parentDependencies = parentDependencies;
			this.commands = commands;
			this.server = server;
		}
	}
	
	@Override
	public void register() {
		CommandManager cmdManager = dependencies.server.getCommandManager();
		cmdManager.register(cmdManager.metaBuilder(name).build(), this);
	}

	@Override
	public void unregister() {
		CommandManager cmdManager = dependencies.server.getCommandManager();
		cmdManager.unregister(name);
	}
	
	private CmdSender adaptSender(CommandSource platformSender) {
		VelocityCmdSender.CmdSenderDependencies parentDependencies = dependencies.parentDependencies;
		return (platformSender instanceof Player) ?
				new VelocityCmdSender.PlayerSender(parentDependencies, (Player) platformSender)
				: new VelocityCmdSender.ConsoleSender(parentDependencies, platformSender);
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
		dependencies.commands.execute(adaptSender(platformSender), new ArrayCommandPackage(name, adaptArgs(args)));
	}
	
	@Override
	public List<String> suggest(Invocation invocation) {
		CommandSource platformSender = invocation.source();
		String[] args = invocation.arguments();
		return dependencies.commands.suggest(adaptSender(platformSender), adaptArgs(args));
	}
	
}
