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

package space.arim.libertybans.core.database.pagination;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LongBorderValueTest {

    private final LongBorderValue borderValue = new LongBorderValue();

    @Test
    public void run() {
        long subject = 4L;
        String[] output = new String[borderValue.len()];
        borderValue.writeChatCode(subject, new ArrayCursor(output, 0));
        assertEquals(subject, borderValue.readChatCode(new ArrayCursor(output, 0)));
    }

    @RepeatedTest(5)
    public void roundTrip() {
        long subject = ThreadLocalRandom.current().nextLong();
        String[] buffer = new String[borderValue.len()];
        borderValue.writeChatCode(subject, new ArrayCursor(buffer, 0));
        assertEquals(subject, borderValue.readChatCode(new ArrayCursor(buffer, 0)));
    }
}
