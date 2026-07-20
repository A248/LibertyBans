/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

public final class StringCommandPackage implements CommandPackage {

	private final String args;
	/** Position never refers to a hidden command argument */
	private transient int position;
	private final Map<String, String> hiddenArguments = new HashMap<>();

	private StringCommandPackage(String args) {
		this.args = args;
	}

	/**
	 * Creates from arguments
	 *
	 * @param args the arguments, must not be null
	 * @return the command package
	 */
	public static StringCommandPackage create(String args) {
		StringCommandPackage commandPackage = new StringCommandPackage(args);
		commandPackage.consumeHiddenArguments();
		return commandPackage;
	}

	private int computeNextAdvance() {
		int pos = position;
		while (pos < args.length() && args.charAt(pos) != ' ') {
			pos++;
		}
		return pos - position;
	}

	private String consumeCurrentArgument() {
		int initialPos = position;
		int nextAdvance = computeNextAdvance();
		position += nextAdvance;
		// 1. Skip the space character before moving onto the next argument
		if (position < args.length() && args.charAt(position) == ' '
				// 2. Consume trailing empty argument only once
				|| position == args.length()) {
			position++;
		}
		return args.substring(initialPos, initialPos + nextAdvance);
	}

	// Maintains the guarantee that position never refers to a hidden argument
	private void consumeHiddenArguments() {
		while (true) {
			if (position >= args.length()) {
				return;
			}
			if (args.charAt(position) != HIDDEN_ARG_PREFIX) {
				return;
			}
			// Position on the first character of the hidden argument
			position++;
			// Collect the hidden argument as if it were a plain argument
			String hiddenArgument = consumeCurrentArgument();
			// Then parse it and add it to our known collection
			String[] hiddenArgPieces = hiddenArgument.split("=", 2);
			hiddenArguments.put(
					hiddenArgPieces[0].toLowerCase(Locale.ROOT),
					hiddenArgPieces.length == 2 ? hiddenArgPieces[1] : null
			);
			// At this point we will be positioned on the next argument
		}
	}

	@Override
	public String next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		String thisArg = consumeCurrentArgument();
		consumeHiddenArguments();
		return thisArg;
	}

	@Override
	public String peek() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		int initialPos = position;
		int nextAdvance = computeNextAdvance();
		return args.substring(initialPos, initialPos + nextAdvance);
	}

	@Override
	public boolean hasNext() {
		return position <= args.length();
	}

	@Override
	public boolean findHiddenArgument(String argument) {
		return hiddenArguments.containsKey(argument);
	}

	@Override
	public @Nullable String findHiddenArgumentSpecifiedValue(String argPrefix) {
		return hiddenArguments.get(argPrefix);
	}

	@Override
	public String allRemaining() {
		if (position > args.length()) {
			return "";
		}
		String allRemaining = args.substring(position);
		position = args.length() + 1;
		return allRemaining;
	}

	@Override
	public CommandPackage copy() {
		StringCommandPackage copy = new StringCommandPackage(args);
		copy.position = position;
		copy.hiddenArguments.putAll(hiddenArguments);
		return copy;
	}

	@Override
	public String toString() {
		return "StringCommandPackage{" +
				"args='" + args + '\'' +
				", position=" + position +
				", hiddenArguments=" + hiddenArguments +
				'}';
	}
}
