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

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import space.arim.bans.api.Subject;

public class BukkitCommands implements AutoCloseable, CommandExecutor {
	private final BukkitEnv environment;

	public BukkitCommands(final BukkitEnv environment) {
		this.environment = environment;
		refreshConfig();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Subject subject;
		if (sender instanceof Player) {
			subject = environment.center().subjects().parseSubject(((Player) sender).getUniqueId());
		} else if (sender instanceof ConsoleCommandSender) {
			subject = Subject.console();
		} else {
			return true;
		}
		if (args.length > 0) {
			this.environment.center().commands().execute(subject, args);
		} else {
			this.environment.center().commands().usage(subject);
		}
		return true;
	}

	public void refreshConfig() {
		
	}
	
	@Override
	public void close() {
		
	}
}
