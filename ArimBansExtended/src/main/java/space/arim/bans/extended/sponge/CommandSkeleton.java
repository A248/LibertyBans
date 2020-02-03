/*
 * ArimBansExtended, an extension for the ArimBans core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
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
package space.arim.bans.extended.sponge;

import java.util.List;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.Player;

import space.arim.bans.api.Subject;
import space.arim.bans.extended.ArimBansExtendedPlugin;

import space.arim.api.server.sponge.DecoupledCommand;

public class CommandSkeleton extends DecoupledCommand {
	
	private final ArimBansExtendedPlugin plugin;
	private final String cmd;
	
	public CommandSkeleton(ArimBansExtendedPlugin plugin, String cmd) {
		this.plugin = plugin;
		this.cmd = cmd;
	}
	
	@Override
	protected boolean execute(CommandSource sender, String[] args) {
		if (plugin.enabled()) {
			Subject subject;
			if (sender instanceof Player) {
				subject = plugin.extension().getLib().fromUUID(((Player) sender).getUniqueId());
			} else if (sender instanceof ConsoleSource) {
				subject = Subject.console();
			} else {
				return false;
			}
			plugin.extension().fireCommand(subject, cmd, args);
		}
		return false;
	}
	
	@Override
	protected List<String> getTabComplete(CommandSource sender, String[] args) {
		return plugin.getTabComplete(args);
	}
	
}
