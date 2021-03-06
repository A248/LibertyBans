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

import java.util.Iterator;
import java.util.Locale;

public abstract class CommandPackage implements Iterator<String> {

	private final String command;
	
	/**
	 * Creates from the true name of the base command executed
	 * 
	 * @param command the true base command executed
	 */
	public CommandPackage(String command) {
		this.command = command;
	}
	
	/**
	 * Gets the true base command run, always lowercase
	 * 
	 * @return the true base command, lowercased
	 */
	public String getCommand() {
		return command.toLowerCase(Locale.ROOT);
	}
	
	/**
	 * Gets the current argument and advances to the next argument
	 * 
	 * @return the current argument
	 */
	@Override
	public abstract String next();
	
	/**
	 * Gets the current argument without advancing to the next one
	 * 
	 * @return the current argument
	 */
	public abstract String peek();
	
	/**
	 * Indicates whether there are more arguments.
	 * 
	 * @return true if there are more arguments, false otherwise
	 */
	@Override
	public abstract boolean hasNext();
	
	/**
	 * Concatenates the current argument and all remaining arguments. This would
	 * be equivalent to joining all calls to {@link #next()}, separating with spaces,
	 * until this iterator is exhausted.
	 * 
	 * @return the concatenated result
	 */
	public abstract String allRemaining();
	
	/**
	 * Creates an identical copy of this command package. Mutating this object
	 * or the produced copy will not affect the other.
	 * 
	 */
	public abstract CommandPackage copy();
	
}
