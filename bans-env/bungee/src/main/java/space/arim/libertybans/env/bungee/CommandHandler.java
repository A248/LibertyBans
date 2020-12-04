/* 
 * LibertyBans-env-bungee
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-bungee is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-bungee is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-bungee. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.bungee;

import jakarta.inject.Inject;

import space.arim.omnibus.util.ArraysUtil;

import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.PlatformListener;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;

public class CommandHandler extends Command implements TabExecutor, PlatformListener {

	private final DependencyPackage dependencies;
	private final boolean alias;
	
	CommandHandler(DependencyPackage dependencies, String command, boolean alias) {
		super(command);
		this.dependencies = dependencies;
		this.alias = alias;
	}
	
	public static class DependencyPackage {
		
		final BungeeCmdSender.CmdSenderDependencies parentDependencies;
		final Commands commands;
		final Plugin plugin;
		
		@Inject
		public DependencyPackage(BungeeCmdSender.CmdSenderDependencies parentDependencies, Commands commands,
				Plugin plugin) {
			this.parentDependencies = parentDependencies;
			this.commands = commands;
			this.plugin = plugin;
		}
	}
	
	@Override
	public void register() {
		Plugin plugin = dependencies.plugin;
		plugin.getProxy().getPluginManager().registerCommand(plugin, this);
	}
	
	@Override
	public void unregister() {
		dependencies.plugin.getProxy().getPluginManager().unregisterCommand(this);
	}
	
	private CmdSender adaptSender(CommandSender platformSender) {
		BungeeCmdSender.CmdSenderDependencies parentDependencies = dependencies.parentDependencies;
		return (platformSender instanceof ProxiedPlayer) ?
				new BungeeCmdSender.PlayerSender(parentDependencies, (ProxiedPlayer) platformSender)
				: new BungeeCmdSender.ConsoleSender(parentDependencies, platformSender);
	}
	
	private String[] adaptArgs(String[] args) {
		if (alias) {
			return ArraysUtil.expandAndInsert(args, getName(), 0);
		}
		return args;
	}

	@Override
	public void execute(CommandSender platformSender, String[] args) {
		dependencies.commands.execute(adaptSender(platformSender), new ArrayCommandPackage(getName(), adaptArgs(args)));
	}

	@Override
	public Iterable<String> onTabComplete(CommandSender platformSender, String[] args) {
		return dependencies.commands.suggest(adaptSender(platformSender), adaptArgs(args));
	}
	
}
