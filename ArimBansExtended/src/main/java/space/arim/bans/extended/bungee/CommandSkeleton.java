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
package space.arim.bans.extended.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import space.arim.bans.api.Subject;
import space.arim.bans.extended.ArimBansExtendedPlugin;

public class CommandSkeleton extends Command implements TabExecutor {

	private final ArimBansExtendedPlugin plugin;
	
	public CommandSkeleton(ArimBansExtendedPlugin plugin, String cmd) {
		super(cmd);
		this.plugin = plugin;
	}
	
	@Override
	public void execute(CommandSender sender, String[] args) {
		if (plugin.enabled()) {
			Subject subject;
			if (sender instanceof ProxiedPlayer) {
				subject = plugin.extension().getLib().fromUUID(((ProxiedPlayer) sender).getUniqueId());
			} else {
				subject = Subject.console();
			}
			plugin.extension().fireCommand(subject, getName(), args);
		}
	}
	
	@Override
	public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
		return plugin.getTabComplete(args);
	}
	
}
