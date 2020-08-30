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

import space.arim.omnibus.util.ArraysUtil;

import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.PlatformListener;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

class CommandHandler implements SimpleCommand, PlatformListener {

	private final VelocityEnv env;
	private final String name;
	private final boolean alias;
	
	CommandHandler(VelocityEnv env, String name, boolean alias) {
		this.env = env;
		this.name = name;
		this.alias = alias;
	}
	
	@Override
	public void register() {
		CommandManager cmdManager = env.getServer().getCommandManager();
		cmdManager.register(cmdManager.metaBuilder(name).build(), this);
	}

	@Override
	public void unregister() {
		CommandManager cmdManager = env.getServer().getCommandManager();
		cmdManager.unregister(name);
	}
	
	private CmdSender adaptSender(CommandSource platformSender) {
		return (platformSender instanceof Player) ?
				new PlayerCmdSender(env, (Player) platformSender)
				: new ConsoleCmdSender(env, platformSender);
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
		env.core.getCommands().execute(adaptSender(platformSender), new ArrayCommandPackage(name, adaptArgs(args)));
	}
	
	@Override
	public List<String> suggest(Invocation invocation) {
		CommandSource platformSender = invocation.source();
		String[] args = invocation.arguments();
		return env.core.getCommands().suggest(adaptSender(platformSender), adaptArgs(args));
	}
	
}
