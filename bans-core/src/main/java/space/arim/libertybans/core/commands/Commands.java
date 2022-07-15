/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.core.commands;

import java.util.List;

import space.arim.libertybans.core.env.CmdSender;

public interface Commands {

	String BASE_COMMAND_NAME = "libertybans";

	/**
	 * Executes a command
	 *
	 * @param sender the command sender
	 * @param command the command
	 */
	void execute(CmdSender sender, CommandPackage command);

	/**
	 * Requests tab completions
	 *
	 * @param sender the command sender
	 * @param args The argument array. This must include the command name as the first
	 *             element in the array. This must also include trailing empty elements
	 *             if the player is tab completing the latest argument in wildcard fashion.
	 * @return the tab completions
	 */
	List<String> suggest(CmdSender sender, String[] args);

	/**
	 * Determines whether a sender has permission for a command, for tab completion purposes
	 *
	 * @param sender the command sender
	 * @param command the command name, must be lowercase
	 * @return true if the sender has permission to use the alias
	 */
	boolean hasPermissionFor(CmdSender sender, String command);

}
