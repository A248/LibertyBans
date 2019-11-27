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
package space.arim.bans.internal.frontend.commands;

import space.arim.bans.api.CommandType;
import space.arim.bans.api.Subject;
import space.arim.bans.internal.Component;

public interface CommandsMaster extends Component {
	@Override
	default Class<?> getType() {
		return CommandsMaster.class;
	}
	
	void execute(Subject subject, String[] rawArgs);
	
	void execute(Subject subject, CommandType command, String[] extraArgs);

	default CommandType parseCommand(String input) {
		switch (input.toLowerCase()) {
		// Special cases go here
		case "playerbanlist":
			return CommandType.UUIDBANLIST;
		case "playermutelist":
			return CommandType.UUIDMUTELIST;
		// Otherwise parse normally
		default:
			for (CommandType type : CommandType.values()) {
				if (type.toString().equalsIgnoreCase(input)) {
					return type;
				}
			}
			throw new IllegalArgumentException("Input '" + input + "' could not be parsed as a CommandType!");
		}
	}

	void usage(Subject subject);
	
	void usage(Subject subject, CommandType command);
	
	void noPermission(Subject subject);
	
	void noPermission(Subject subject, CommandType command);

}
