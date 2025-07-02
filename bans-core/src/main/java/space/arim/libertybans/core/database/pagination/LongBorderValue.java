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

public record LongBorderValue() implements BorderValueHandle<Long> {
    @Override
    public int len() {
        return 1;
    }

    @Override
    public void writeChatCode(@NonNull Long value, @NonNull Write write) {
        write.writePart(Long.toUnsignedString(value, Character.MAX_RADIX));
    }

    @Override
    public @Nullable Long readChatCode(@NonNull Read read) {
        try {
            return Long.parseUnsignedLong(read.readPart(), Character.MAX_RADIX);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
