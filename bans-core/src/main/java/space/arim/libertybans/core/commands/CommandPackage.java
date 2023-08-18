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

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;

/**
 * A command argument iterator. Defines several means for accessing
 * command arguments: <br>
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
public interface CommandPackage extends Iterator<String> {

	/**
	 * The prefix denoting a hidden argument
	 */
	char HIDDEN_ARG_PREFIX = '-';

	/**
	 * Gets the current argument and advances to the next argument
	 * 
	 * @return the current argument
	 */
	@Override
	String next();

	/**
	 * Gets the current argument without advancing to the next one
	 * 
	 * @return the current argument
	 */
	String peek();

	/**
	 * Indicates whether there are more arguments.
	 * 
	 * @return true if there are more arguments, false otherwise
	 */
	@Override
	boolean hasNext();

	/**
	 * Finds a certain hidden argument. See the class javadoc for the meaning of hidden arguments. <br>
	 * <br>
	 * The meaning of a hidden argument will depend on the implementation.
	 * It is typical to use "-" as a special leading character to indicate hidden arguments.
	 *
	 * @param argument the hidden argument, excluding any special leading characters. Case insensitive
	 * @return whether the hidden argument is present in the arguments which have been encountered so far
	 */
	boolean findHiddenArgument(String argument);

	/**
	 * Finds a certain hidden argument such as "arg=value" and yields the associated value.
	 * This is akin to using {@link #findHiddenArgument(String)} except the argument is a wildcard
	 * wih respect to the specified value.
	 *
	 * @param argPrefix the first part of the hidden argument, i.e. "arg" in "arg=value"
	 * @return the value if it exists
	 */
	@Nullable String findHiddenArgumentSpecifiedValue(String argPrefix);

	/**
	 * Concatenates the current argument and all remaining arguments. This would
	 * be equivalent to joining all calls to {@link #next()}, separating with spaces,
	 * until this iterator is exhausted.
	 * 
	 * @return the concatenated result
	 */
	String allRemaining();

	/**
	 * Creates an identical copy of this command package. Mutating this object
	 * or the produced copy will not affect the other.
	 *
	 * @return the copy
	 */
	CommandPackage copy();

}
