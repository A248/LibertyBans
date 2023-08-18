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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandPackageTest {

	@ParameterizedTest
	@ArgumentsSource(CommandPackageImpl.Provider.class)
	public void noArguments(CommandPackageImpl impl) {
		CommandPackage cmd = impl.createEmpty();

		assertFalse(cmd.hasNext());
		assertEquals("", cmd.allRemaining());
		assertFalse(cmd.findHiddenArgument("s"));
		assertThrows(NoSuchElementException.class, cmd::next);
	}

	@ParameterizedTest
	@ArgumentsSource(CommandPackageImpl.Provider.class)
	public void oneNormalArg(CommandPackageImpl impl) {
		CommandPackage cmd = impl.create("arg");

		assertTrue(cmd.hasNext());
		assertFalse(cmd.findHiddenArgument("g"));
		assertEquals("arg", cmd.next());
		assertThrows(NoSuchElementException.class, cmd::next);
	}

	@ParameterizedTest
	@ArgumentsSource(CommandPackageImpl.Provider.class)
	public void oneNormalArgWithPeek(CommandPackageImpl impl) {
		CommandPackage cmd = impl.create("arg");

		assertTrue(cmd.hasNext());
		assertEquals("arg", cmd.peek());
		assertTrue(cmd.hasNext());
		assertEquals("arg", cmd.next());
		assertThrows(NoSuchElementException.class, cmd::next);
	}

	@ParameterizedTest
	@ArgumentsSource(CommandPackageImpl.Provider.class)
	public void oneNormalArgAllRemaining(CommandPackageImpl impl) {
		CommandPackage cmd = impl.create("arg");

		assertEquals("arg", cmd.allRemaining());
		assertThrows(NoSuchElementException.class, cmd::next);
	}

	@ParameterizedTest
	@ArgumentsSource(CommandPackageImpl.Provider.class)
	public void oneNormalArgAllRemainingWithPeek(CommandPackageImpl impl) {
		CommandPackage cmd = impl.create("arg");

		assertEquals("arg", cmd.peek());
		assertEquals("arg", cmd.allRemaining());
		assertThrows(NoSuchElementException.class, cmd::next);
	}

	@ParameterizedTest
	@ArgumentsSource(CommandPackageImpl.Provider.class)
	public void hiddenArgSurroundedByNormalArgsConsume(CommandPackageImpl impl) {
		CommandPackage cmd = impl.create("arg1 -s arg2");

		assertFalse(cmd.findHiddenArgument("s"), "-s is not yet visible");
		assertFalse(cmd.findHiddenArgument("g"), "No such -g specified");

		assertEquals("arg1", cmd.peek());
		assertEquals("arg1", cmd.next());

		assertTrue(cmd.findHiddenArgument("s"), "-s is now visible, having been passed");
		assertFalse(cmd.findHiddenArgument("g"), "No such -g specified");
	}

	@ParameterizedTest
	@ArgumentsSource(CommandPackageImpl.Provider.class)
	public void hiddenArgSurroundedByNormalArgsAllRemaining(CommandPackageImpl impl) {
		CommandPackage cmd = impl.create("arg1 -s arg2");

		assertFalse(cmd.findHiddenArgument("s"), "-s is not yet visible");
		assertFalse(cmd.findHiddenArgument("g"), "No such -g specified");

		assertEquals("arg1", cmd.peek());
		assertEquals("arg1 -s arg2", cmd.allRemaining());
	}

	@ParameterizedTest
	@ArgumentsSource(CommandPackageImpl.Provider.class)
	public void hiddenArgOnlyCanBeFound(CommandPackageImpl impl) {
		CommandPackage cmd = impl.create("-gav");

		assertTrue(cmd.findHiddenArgument("gav"), "Hidden argument can be found even if no normal arguments exist");
	}

	@ParameterizedTest
	@ArgumentsSource(CommandPackageImpl.Provider.class)
	public void hiddenArgOnlyHasNoNormalArguments(CommandPackageImpl impl) {
		CommandPackage cmd = impl.create("-gav");

		assertFalse(cmd.hasNext(), "Hidden arguments are not visible through iteration");
		assertEquals("", cmd.allRemaining());
	}

	@ParameterizedTest
	@ArgumentsSource(CommandPackageImpl.Provider.class)
	public void emptyHiddenArgument(CommandPackageImpl impl) {
		CommandPackage cmd = impl.create("arg1 -");

		assertEquals("arg1", cmd.next());
		assertTrue(cmd.findHiddenArgument(""));
	}

	@ParameterizedTest
	@ArgumentsSource(CommandPackageImpl.Provider.class)
	public void multipleNormalAndHiddenArgs(CommandPackageImpl impl) {
		CommandPackage cmd = impl.create("user1 -s 30d teaming -green in kitpvp");

		assertTrue(cmd.hasNext());
		assertEquals("user1", cmd.next());

		assertTrue(cmd.hasNext());
		assertEquals("30d", cmd.next());

		assertTrue(cmd.findHiddenArgument("s"));
		assertFalse(cmd.findHiddenArgument("green"));

		assertEquals("teaming -green in kitpvp", cmd.allRemaining());
		assertFalse(cmd.hasNext());
	}

	@ParameterizedTest
	@ArgumentsSource(CommandPackageImpl.Provider.class)
	public void multipleNormalAndHiddenArgsWithPeek(CommandPackageImpl impl) {
		CommandPackage cmd = impl.create("user1 -s 30d teaming -green in kitpvp");

		assertTrue(cmd.hasNext());
		assertEquals("user1", cmd.peek());
		assertEquals("user1", cmd.next());

		assertTrue(cmd.hasNext());
		assertEquals("30d", cmd.peek());
		assertEquals("30d", cmd.next());

		assertTrue(cmd.findHiddenArgument("s"));
		assertFalse(cmd.findHiddenArgument("green"));

		assertEquals("teaming", cmd.peek());
		assertEquals("teaming -green in kitpvp", cmd.allRemaining());
		assertFalse(cmd.hasNext());
	}

	@ParameterizedTest
	@ArgumentsSource(CommandPackageImpl.Provider.class)
	public void hiddenArgSpecifiedValue(CommandPackageImpl impl) {
		CommandPackage cmd = impl.create("user1 -s -scope=hello 30d teaming -green in kitpvp");

		assertFalse(cmd.findHiddenArgument("s"), "-s not yet visible");
		assertNull(cmd.findHiddenArgumentSpecifiedValue("scope"), "scope not yet visible");
		assertNull(cmd.findHiddenArgumentSpecifiedValue("track"));

		assertTrue(cmd.hasNext());
		assertEquals("user1", cmd.next());

		assertTrue(cmd.hasNext());
		assertEquals("30d", cmd.next());

		assertTrue(cmd.findHiddenArgument("s"));
		assertEquals("hello", cmd.findHiddenArgumentSpecifiedValue("scope"));
		assertFalse(cmd.findHiddenArgument("green"));
		assertNull(cmd.findHiddenArgumentSpecifiedValue("track"));

		assertEquals("teaming -green in kitpvp", cmd.allRemaining());
		assertFalse(cmd.hasNext());
	}

}
