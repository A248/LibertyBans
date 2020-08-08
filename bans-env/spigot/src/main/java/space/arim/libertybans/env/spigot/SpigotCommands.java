/* 
 * LibertyBans-env-spigot
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-spigot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-spigot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-spigot. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.spigot;

import space.arim.libertybans.core.commands.ArrayCommandPackage;
import space.arim.libertybans.core.env.CmdSender;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class SpigotCommands implements CommandExecutor {

	private final SpigotEnv env;
	
	SpigotCommands(SpigotEnv env) {
		this.env = env;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		CmdSender iSender;
		if (sender instanceof Player) {
			iSender = new PlayerCmdSender(env, (Player) sender);
		} else {
			iSender = new ConsoleCmdSender(env, sender);
		}
		env.core.getCommands().execute(iSender, new ArrayCommandPackage(command.getName(), args));
		return true;
	}

}
