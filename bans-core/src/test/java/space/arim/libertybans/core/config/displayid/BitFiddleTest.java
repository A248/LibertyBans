/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

package space.arim.libertybans.core.config.displayid;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BitFiddleTest {

    private final Scramble fiddle = new Scramble.BitFiddle();

    private static void assertEq(long expected, long actual) {
        assertEquals(expected, actual, () -> {
            String expectedStr = Long.toUnsignedString(expected, 2);
            String actualStr = Long.toUnsignedString(actual, 2);
            return "Expected " + expectedStr + " but got " + actualStr;
        });
    }

    @Test
    public void scramble() {
        assertEq(0b10101010101010101010101010101010_10101010101010101010101010101010L,
                fiddle.scramble(0b00000000000000000000000000000000_11111111111111111111111111111111L));
    }

    @Test
    public void descramble() {
        assertEq(0b00000000000000000000000000000000_11111111111111111111111111111111L,
                fiddle.descramble(0b10101010101010101010101010101010_10101010101010101010101010101010L));
    }

    @RepeatedTest(10)
    public void randomRoundTrip() {
        long subject = ThreadLocalRandom.current().nextLong();
        long roundTrip = fiddle.descramble(fiddle.scramble(subject));
        assertEq(subject, roundTrip);
    }

    @Test
    public void pseudoRandomDisplay() {
        assertEquals("8", Long.toUnsignedString(fiddle.scramble(fiddle.scramble(1)), 19));
        assertEquals("6e", Long.toUnsignedString(fiddle.scramble(fiddle.scramble(2)), 19));
        assertEquals("73", Long.toUnsignedString(fiddle.scramble(fiddle.scramble(3)), 19));
        assertEquals("5cf", Long.toUnsignedString(fiddle.scramble(fiddle.scramble(4)), 19));
        assertEquals("5d4", Long.toUnsignedString(fiddle.scramble(fiddle.scramble(5)), 19));
        assertEquals("60a", Long.toUnsignedString(fiddle.scramble(fiddle.scramble(6)), 19));
        assertEquals("5188", Long.toUnsignedString(fiddle.scramble(fiddle.scramble(12)), 19));
        assertEquals("51f3", Long.toUnsignedString(fiddle.scramble(fiddle.scramble(14)), 19));
        assertEquals("2089i54b5", Long.toUnsignedString(fiddle.scramble(fiddle.scramble(297)), 19));
        assertEquals("2089i54hb", Long.toUnsignedString(fiddle.scramble(fiddle.scramble(298)), 19));
    }
}
