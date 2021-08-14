/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.commands;

import java.util.stream.Stream;

import space.arim.libertybans.core.env.CmdSender;

/**
 * A subcommand
 * 
 * @author A248
 *
 */
interface SubCommandGroup {
	
	/**
	 * Whether this subcommand object matches the specified subcommand argument
	 * 
	 * @param arg the argument, lowercased
	 * @return true if matches, false otherwise
	 */
	boolean matches(String arg);

	/**
	 * Executes the subcommand, assuming it matched the subcommand argument
	 * 
	 * @param sender the command sender
	 * @param command the command
	 * @param arg the argument matched to this sub command group, lowercased
	 * @return the execution
	 */
	CommandExecution execute(CmdSender sender, CommandPackage command, String arg);
	
	/**
	 * Gets tab complete suggestions for the subcommand
	 * 
	 * @param sender the command sender
	 * @param arg the argument matched to this sub command group, lowercased
	 * @param argIndex the index of the furthest subcommand argument
	 * @return tab complete suggestions
	 */
	Stream<String> suggest(CmdSender sender, String arg, int argIndex);
	
}
