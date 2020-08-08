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

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

class VelocityCommands implements SimpleCommand {

	private final VelocityEnv env;
	
	VelocityCommands(VelocityEnv env) {
		this.env = env;
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource sender = invocation.source();
		String[] args = invocation.arguments();
		CmdSender iSender;
		if (sender instanceof Player) {
			iSender = new PlayerCmdSender(env, (Player) sender);
		} else {
			iSender = new ConsoleCmdSender(env, sender);
		}
		env.core.getCommands().execute(iSender, new ArrayCommandPackage(Commands.BASE_COMMAND_NAME, args));
	}
	
}
