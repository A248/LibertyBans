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

import java.time.Instant;

public record InstantBorderValue(BorderValueHandle<Long> longBorderValue) implements BorderValueHandle<Instant> {

    @Override
    public int len() {
        return longBorderValue.len();
    }

    @Override
    public void writeChatCode(@NonNull Instant value, @NonNull Write write) {
        longBorderValue.writeChatCode(value.getEpochSecond(), write);
    }

    @Override
    public @Nullable Instant readChatCode(@NonNull Read read) {
        Long seconds = longBorderValue.readChatCode(read);
        return seconds == null ? null : Instant.ofEpochSecond(seconds);
    }
}
