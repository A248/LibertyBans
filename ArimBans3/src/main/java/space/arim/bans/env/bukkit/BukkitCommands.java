/*
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.env.bukkit;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import space.arim.bans.api.Subject;
import space.arim.bans.internal.Configurable;

import space.arim.api.server.bukkit.SpigotUtil;

public class BukkitCommands implements Configurable, CommandExecutor, TabCompleter {
	
	private final BukkitEnv environment;

	public BukkitCommands(final BukkitEnv environment) {
		this.environment = environment;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Subject subject;
		if (sender instanceof Player) {
			subject = environment.center().subjects().parseSubject(((Player) sender).getUniqueId());
		} else if (sender instanceof ConsoleCommandSender) {
			subject = Subject.console();
		} else {
			return false;
		}
		environment.center().commands().execute(subject, args);
		return true;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		return SpigotUtil.getPlayerNameTabComplete(args, environment.plugin().getServer());
	}
	
}
