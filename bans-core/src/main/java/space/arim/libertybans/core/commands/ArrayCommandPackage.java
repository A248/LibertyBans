/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringJoiner;

public final class ArrayCommandPackage implements CommandPackage {

	private final String[] args;
	/** Position never refers to a hidden command argument */
	private transient int position;

	private ArrayCommandPackage(String[] args) {
		this.args = args;
	}

	/**
	 * Creates from an argument array. The input array is NOT cloned
	 *
	 * @param args the argument array, of which no elements can be null
	 * @return the command package
	 */
	public static ArrayCommandPackage create(String...args) {
		ArrayCommandPackage commandPackage = new ArrayCommandPackage(args);
		commandPackage.movePastHiddenArguments();
		return commandPackage;
	}

	// Maintains the guarantee that position never refers to a hidden argument
	private void movePastHiddenArguments() {
		String currentArg;
		while (position < args.length
				&& !(currentArg = args[position]).isEmpty()
				&& currentArg.charAt(0) == HIDDEN_ARG_PREFIX) {
			position++;
		}
	}
	
	@Override
	public String next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		String thisArg = args[position++];
		movePastHiddenArguments();
		return thisArg;
	}
	
	@Override
	public String peek() {
		return args[position];
	}
	
	@Override
	public boolean hasNext() {
		return position != args.length;
	}

	@Override
	public boolean findHiddenArgument(String argument) {
		String searchFor = '-' + argument;
		for (int n = 0; n < position; n++) {
			if (args[n].equalsIgnoreCase(searchFor)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String findHiddenArgumentSpecifiedValue(String argPrefix) {
		String searchFor = '-' + argPrefix + '=';
		for (int n = 0; n < position; n++) {
			if (args[n].toLowerCase(Locale.ROOT).startsWith(searchFor)) {
				return args[n].substring(searchFor.length());
			}
		}
		return null;
	}

	@Override
	public String allRemaining() {
		StringJoiner joiner = new StringJoiner(" ");
		for (int n = position; n < args.length; n++) {
			joiner.add(args[n]);
		}
		position = args.length;
		return joiner.toString();
	}

	@Override
	public CommandPackage copy() {
		ArrayCommandPackage copy = new ArrayCommandPackage(args);
		copy.position = position;
		return copy;
	}

}
