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

import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.PlatformListener;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

class CommandHandler implements SimpleCommand, PlatformListener {

	private final VelocityEnv env;
	
	CommandHandler(VelocityEnv env) {
		this.env = env;
	}
	
	@Override
	public void register() {
		CommandManager cmdManager = env.getServer().getCommandManager();
		cmdManager.register(cmdManager.metaBuilder(Commands.BASE_COMMAND_NAME).build(), this);
	}

	@Override
	public void unregister() {
		CommandManager cmdManager = env.getServer().getCommandManager();
		cmdManager.unregister(Commands.BASE_COMMAND_NAME);
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource platformSender = invocation.source();
		String[] args = invocation.arguments();
		CmdSender sender;
		if (platformSender instanceof Player) {
			sender = new PlayerCmdSender(env, (Player) platformSender);
		} else {
			sender = new ConsoleCmdSender(env, platformSender);
		}
		env.core.getCommands().execute(sender, new ArrayCommandPackage(Commands.BASE_COMMAND_NAME, args));
	}
	
}
