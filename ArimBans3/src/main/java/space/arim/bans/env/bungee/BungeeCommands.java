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
package space.arim.bans.env.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.command.ConsoleCommandSender;
import space.arim.bans.api.Subject;

public class BungeeCommands extends Command implements AutoCloseable {

	private final BungeeEnv environment;
	
	public BungeeCommands(final BungeeEnv environment) {
		super("arimbans");
		this.environment = environment;
		refreshConfig();
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		Subject subject;
		if (sender instanceof ProxiedPlayer) {
			subject = environment.center().subjects().parseSubject(((ProxiedPlayer) sender).getUniqueId());
		} else if (sender instanceof ConsoleCommandSender){
			subject = Subject.console();
		} else {
			return;
		}
		environment.center().async(() -> {
			if (args.length > 0) {
				environment.center().commands().execute(subject, args);
			} else {
				environment.center().commands().usage(subject);
			}
		});
	}
	
	@Override
	public void close() {
		
	}
	
	public void refreshConfig() {
		
	}

}
