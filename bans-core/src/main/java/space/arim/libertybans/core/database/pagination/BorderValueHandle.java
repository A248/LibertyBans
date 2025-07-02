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

/**
 * Interface for dealing with border values.
 *
 */
public interface BorderValueHandle<V> {

    /**
     * The number of elements that need to be stored as parts
     *
     * @return the length
     */
    int len();

    /**
     * Converts into a chat code. Places it in the output writer, sending exactly as many parts as {@code len()}
     *
     * @param value the value
     * @param write the parts writer
     */
    void writeChatCode(@NonNull V value, @NonNull Write write);

    /**
     * Converts from a chat code, or returns null upon failure. Should use exactly as many parts as {@code len()}
     * @param read the parts reader
     * @return the value, or null for failure
     */
    @Nullable V readChatCode(@NonNull Read read);

    interface Write {
        void writePart(String part);
    }

    interface Read {
        String readPart();
    }
}
