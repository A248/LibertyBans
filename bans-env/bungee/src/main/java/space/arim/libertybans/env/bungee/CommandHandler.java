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

import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.commands.Commands;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.env.PlatformListener;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

class CommandHandler extends Command implements PlatformListener {

	private final BungeeEnv env;
	
	CommandHandler(BungeeEnv env) {
		super(Commands.BASE_COMMAND_NAME);
		this.env = env;
	}
	
	@Override
	public void register() {
		env.getPlugin().getProxy().getPluginManager().registerCommand(env.getPlugin(), this);
	}
	
	@Override
	public void unregister() {
		env.getPlugin().getProxy().getPluginManager().unregisterCommand(this);
	}

	@Override
	public void execute(CommandSender platformSender, String[] args) {
		CmdSender sender;
		if (platformSender instanceof ProxiedPlayer) {
			sender = new PlayerCmdSender(env, (ProxiedPlayer) platformSender);
		} else {
			sender = new ConsoleCmdSender(env, platformSender);
		}
		env.core.getCommands().execute(sender, new ArrayCommandPackage(Commands.BASE_COMMAND_NAME, args));
	}

}
