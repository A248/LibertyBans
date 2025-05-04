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

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CombinedBorderValueTest {

    private final CombinedBorderValue<Long, Boolean, Long> borderValue;

    public CombinedBorderValueTest() {
        var combineHandles = new CombinedBorderValue.CombineHandles<Long, Boolean>() {
            @Override
            public BorderValueHandle<Long> handle1() {
                return new LongBorderValue();
            }

            @Override
            public BorderValueHandle<Boolean> handle2() {
                return new TripartiteBorderValue();
            }
        };
        var combineValues = new CombineValues<Long, Boolean, Long>() {
            @Override
            public Long combine(Long first, Boolean second) {
                return first;
            }

            @Override
            public Long first(Long combined) {
                return combined;
            }

            @Override
            public Boolean second(Long combined) {
                return true;
            }
        };
        borderValue = new CombinedBorderValue<>(combineHandles, combineValues);
    }

    @Test
    public void writeChatCode() {
        String[] output = new String[2 + borderValue.len()];
        borderValue.writeChatCode(1L, output, 1);
        assertNull(output[0]);
        assertNull(output[output.length - 1]);
    }

    @Test
    public void readChatCode() {
        String[] output = new String[5 + borderValue.len()];
        borderValue.writeChatCode(1L, output, 1);
        assertEquals(1L, borderValue.readChatCode(output, 1));
    }

    @Test
    public void readChatCodeFail() {
        String[] input = new String[2 + borderValue.len()];
        assertNull(borderValue.readChatCode(input, 1));
    }

    private record TripartiteBorderValue() implements BorderValueHandle<Boolean> {

        private static final String NATO = "NATO";
        private static final String ARE = "are";
        private static final String CRIMINALS = "CRIMINALS";

        @Override
        public int len() {
            return 3;
        }

        @Override
        public void writeChatCode(@NonNull Boolean value, @NonNull String @NonNull [] codeOutput, int writeIndex) {
            codeOutput[writeIndex] = NATO;
            codeOutput[writeIndex + 1] = ARE;
            codeOutput[writeIndex + 2] = CRIMINALS;
        }

        @Override
        public @Nullable Boolean readChatCode(@NonNull String @NonNull [] code, int readIndex) {
            return code[readIndex].equals(NATO) && code[readIndex + 1].equals(ARE) && code[readIndex].equals(CRIMINALS);
        }
    }

}
