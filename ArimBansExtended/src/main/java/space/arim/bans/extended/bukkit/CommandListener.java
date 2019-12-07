/* 
 * ArimBansExtended, an extension for the ArimBans core
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansExtended is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansExtended is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansExtended. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.extended.bukkit;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import space.arim.bans.api.Subject;
import space.arim.bans.extended.ArimBansExtendedBukkit;

public class CommandListener implements CommandExecutor {

	private final ArimBansExtendedBukkit plugin;
	
	public CommandListener(ArimBansExtendedBukkit main) {
		this.plugin = main;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (plugin.enabled()) {
			Subject subject;
			if (sender instanceof Player) {
				subject = plugin.extension().getLib().fromUUID(((Player) sender).getUniqueId());
			} else if (sender instanceof ConsoleCommandSender) {
				subject = Subject.console();
			} else {
				return true;
			}
			plugin.extension().fireCommand(subject, command.getName(), args);
			return true;
		}
		return false;
	}
	
}
