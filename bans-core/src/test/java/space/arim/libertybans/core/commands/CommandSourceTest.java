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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandSourceTest {

    @ParameterizedTest
    @ArgumentsSource(CommandSourceImpl.Provider.class)
    public void noArguments(CommandSourceImpl impl) {
        CommandSource cmd = impl.createEmpty();

        assertFalse(cmd.hasNext());
        assertEquals("", cmd.allRemaining());
        assertThrows(NoSuchElementException.class, cmd::next);
    }

    @ParameterizedTest
    @ArgumentsSource(CommandSourceImpl.Provider.class)
    public void oneNormalArg(CommandSourceImpl impl) {
        CommandSource cmd = impl.create("arg");

        assertTrue(cmd.hasNext());
        assertEquals("arg", cmd.next());
        assertThrows(NoSuchElementException.class, cmd::next);
    }

    @ParameterizedTest
    @ArgumentsSource(CommandSourceImpl.Provider.class)
    public void oneNormalArgWithPeek(CommandSourceImpl impl) {
        CommandSource cmd = impl.create("arg");

        assertTrue(cmd.hasNext());
        assertEquals("arg", cmd.peek());
        assertTrue(cmd.hasNext());
        assertEquals("arg", cmd.next());
        assertThrows(NoSuchElementException.class, cmd::next);
    }

    @ParameterizedTest
    @ArgumentsSource(CommandSourceImpl.Provider.class)
    public void oneNormalArgAllRemaining(CommandSourceImpl impl) {
        CommandSource cmd = impl.create("arg");

        assertEquals("arg", cmd.allRemaining());
        assertThrows(NoSuchElementException.class, cmd::next);
    }

    @ParameterizedTest
    @ArgumentsSource(CommandSourceImpl.Provider.class)
    public void oneNormalArgAllRemainingWithPeek(CommandSourceImpl impl) {
        CommandSource cmd = impl.create("arg");

        assertEquals("arg", cmd.peek());
        assertEquals("arg", cmd.allRemaining());
        assertThrows(NoSuchElementException.class, cmd::next);
    }

    private List<String> collect(CommandSource cmd) {
        List<String> list = new ArrayList<>();
        while (cmd.hasNext()) {
            list.add(cmd.next());
        }
        return list;
    }

    @ParameterizedTest
    @ArgumentsSource(CommandSourceImpl.Provider.class)
    public void trailingEmpty(CommandSourceImpl impl) {
        CommandSource cmd = impl.create("go find ");
        assertEquals("go find ", cmd.copy().allRemaining());
        assertEquals(List.of("go", "find", ""), collect(cmd), () -> "failed for " + impl);
    }

    @ParameterizedTest
    @ArgumentsSource(CommandSourceImpl.Provider.class)
    public void whollyEmpty(CommandSourceImpl impl) {
        CommandSource cmd = impl.create("");
        assertEquals(List.of(""), collect(cmd));
    }
}
