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

import java.util.Set;
import java.util.stream.Stream;

import space.arim.libertybans.core.env.CmdSender;

/**
 * A subcommand
 * 
 * @author A248
 *
 */
public interface SubCommandGroup {

	/**
	 * Gets the sub commands implemented by this sub command group.
	 *
	 * @return the sub commands, all of which should be lowercased
	 */
	Set<String> matches();

	/**
	 * Executes a sub command
	 *
	 * @param sender the command sender
	 * @param command the command
	 * @param arg the sub command matched to this sub command group, lowercased
	 * @return the execution
	 */
	CommandExecution execute(CmdSender sender, CommandPackage command, String arg);

	/**
	 * Gets tab complete suggestions for a sub command. <br>
	 * <br>
	 * The argIndex determines which tab completions are requested. An argIndex of {@code n} means to request
	 * the tab completions for the nth argument of the sub command. The index is zero-based.
	 *
	 * @param sender the command sender
	 * @param arg the sub command matched to this sub command group, lowercased
	 * @param argIndex the index of the furthest argument
	 * @return tab complete suggestions
	 */
	Stream<String> suggest(CmdSender sender, String arg, int argIndex);

	/**
	 * Determines whether the sender has permission for a sub command, for tab completion purposes
	 *
	 * @param sender the command sender
	 * @param arg the argument matched to this sub command group, uppercased
	 * @return true if the sender has permission
	 */
	boolean hasTabCompletePermission(CmdSender sender, String arg);

}
