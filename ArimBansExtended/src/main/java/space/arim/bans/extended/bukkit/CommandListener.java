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

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import space.arim.bans.api.Subject;
import space.arim.bans.extended.ArimBansExtendedPlugin;

public class CommandListener implements CommandExecutor, TabCompleter {

	private final ArimBansExtendedPlugin plugin;
	
	public CommandListener(ArimBansExtendedPlugin plugin) {
		this.plugin = plugin;
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
				return false;
			}
			plugin.extension().fireCommand(subject, command.getName(), args);
			return true;
		}
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		return plugin.getTabComplete(args);
	}
	
}
