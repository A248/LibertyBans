/*
 * ArimBans3, a punishment plugin for minecraft servers
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * ArimBans3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans3. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.env.sponge;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.living.player.Player;

import space.arim.bans.api.Subject;

import space.arim.api.server.sponge.DecoupledCommand;

public class SpongeCommands extends DecoupledCommand {

	private final SpongeEnv environment;
	
	public SpongeCommands(SpongeEnv environment) {
		this.environment = environment;
	}
	
	@Override
	protected boolean execute(CommandSource sender, String[] args) {
		Subject subject;
		if (sender instanceof Player) {
			subject = environment.center().subjects().parseSubject(((Player) sender).getUniqueId());
		} else if (sender instanceof ConsoleSource) {
			subject = Subject.console();
		} else {
			return false;
		}
		environment.center().commands().execute(subject, args);
		return true;
	}
	
}
