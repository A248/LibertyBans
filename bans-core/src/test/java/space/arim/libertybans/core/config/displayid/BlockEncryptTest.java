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

public class BlockEncryptTest {

    @RepeatedTest(5)
    public void longToFromBytes() {
        long subject = ThreadLocalRandom.current().nextLong();
        assertEquals(
                subject,
                Scramble.BlockEncrypt.longFromBytes(Scramble.BlockEncrypt.longToBytes(subject))
        );
    }

    @RepeatedTest(5)
    public void roundTrip() {
        long subject = ThreadLocalRandom.current().nextLong();
        Scramble blockEncrypt = new Scramble.BlockEncrypt();
        assertEquals(subject, blockEncrypt.descramble(blockEncrypt.scramble(subject)));
    }

    @Test
    public void pseudoRandomDisplay() {
        Scramble blockEncrypt = new Scramble.BlockEncrypt();
        assertEquals("280110d19b7e44ed", Long.toUnsignedString(blockEncrypt.scramble(1), 16));
        assertEquals("fa139cc279b2deab", Long.toUnsignedString(blockEncrypt.scramble(42), 16));
        assertEquals("17216cb67318a60b", Long.toUnsignedString(blockEncrypt.scramble(43), 16));
        assertEquals("8a79ab766b7012a", Long.toUnsignedString(blockEncrypt.scramble(450), 16));
    }
}
