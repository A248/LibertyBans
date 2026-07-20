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
import java.util.Objects;

/**
 * A command argument iterator. Defines several means for accessing command arguments: <br>
 * <br>
 * 1. Iterative methods: next, hasNext, and peek.
 * 2. Aggregate methods: allRemaining
 * 3. Hidden argument methods: findHiddenArgument <br>
 * <br>
 * Normal command arguments are accessible via normal iteration (next, hasNext, and peek). <br>
 * <br>
 * There are also hidden arguments, which are not viewable through
 * normal iteration methods. If they have not yet been encountered, hidden arguments are
 * visible through aggregate methods; if they have been encountered, hidden arguments
 * can be accessed with {@link #findHiddenArgument(String)}. A hidden argument is considered
 * "encountered" if the process of iteration has passed the argument in its course.
 *
 */
public final class CommandPackage implements CommandSource {

	private final CommandSource source;
	private final Map<String, String> hiddenArguments;

	/**
	 * The prefix denoting a hidden argument
	 */
	private static final char HIDDEN_ARG_PREFIX = '-';

	private CommandPackage(CommandSource source, Map<String, String> hiddenArguments) {
		this.source = Objects.requireNonNull(source, "source");
		this.hiddenArguments = hiddenArguments;
	}

    public CommandPackage(CommandSource source) {
		this(source, new HashMap<>());
		movePastHiddenArguments();
    }

	// Maintains the guarantee that iterator never refers to a hidden argument
	private void movePastHiddenArguments() {
		String hiddenArg;
		while (source.hasNext()
				&& !(hiddenArg = source.peek()).isEmpty()
				&& hiddenArg.charAt(0) == HIDDEN_ARG_PREFIX) {
			String next = source.next();
			assert hiddenArg.equals(next) : "bad impl source";
			// Then parse it and add it to our known collection
			String[] hiddenArgPieces = hiddenArg.split("=", 2);
			hiddenArguments.put(
					hiddenArgPieces[0].toLowerCase(Locale.ROOT),
					hiddenArgPieces.length == 2 ? hiddenArgPieces[1] : null
			);
		}
	}

	@Override
	public String next() {
		String innerNext = source.next();
		movePastHiddenArguments();
		return innerNext;
	}

	@Override
	public String peek() {
		return source.peek();
	}

	@Override
	public boolean hasNext() {
		return source.hasNext();
	}

	/**
	 * Finds a certain hidden argument. See the class javadoc for the meaning of hidden arguments. <br>
	 * <br>
	 * The meaning of a hidden argument will depend on the implementation.
	 * It is typical to use "-" as a special leading character to indicate hidden arguments.
	 *
	 * @param argument the hidden argument, excluding any special leading characters. Case insensitive
	 * @return whether the hidden argument is present in the arguments which have been encountered so far
	 */
	public boolean findHiddenArgument(String argument) {
		return hiddenArguments.containsKey(argument);
	}

	/**
	 * Finds a certain hidden argument such as "arg=value" and yields the associated value.
	 * This is akin to using {@link #findHiddenArgument(String)} except the argument is a wildcard
	 * wih respect to the specified value.
	 *
	 * @param argPrefix the first part of the hidden argument, i.e. "arg" in "arg=value"
	 * @return the value if it exists
	 */
	public @Nullable String findHiddenArgumentSpecifiedValue(String argPrefix) {
		return hiddenArguments.get(argPrefix);
	}

	@Override
	public CommandPackage copy() {
		CommandSource source = this.source.copy();
		Map<String, String> hiddenArguments = new HashMap<>(this.hiddenArguments);
		return new CommandPackage(source, hiddenArguments);
	}

	@Override
	public String allRemaining() {
		return source.allRemaining();
	}

	/**
	 * Gets remaining arguments, skipping hidden ones, counting them and getting the last argument
	 *
	 * @return the count and last argument
	 */
	public @Nullable CountAndLast countAndLast() {
		if (!hasNext()) {
			return null;
		}
		int count = 0;
		String last;
		do {
			count++;
			last = next();
		} while (hasNext());
		return new CountAndLast(count, last);
	}

	public record CountAndLast(int count, String last) {}

}
