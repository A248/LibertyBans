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
 * <p>
 * The caller to this interface ensures that array arguments are properly sized according to <code>len()</code>
 *
 */
public interface BorderValueHandle<V> {

    /**
     * The number of elements that need to be stored in a string array
     *
     * @return the length
     */
    int len();

    /**
     * Converts into a chat code. Places it in the output array
     *
     * @param value the value
     * @param codeOutput the output array
     * @param writeIndex where to start writing in the output array
     */
    void writeChatCode(@NonNull V value, @NonNull String @NonNull [] codeOutput, int writeIndex);

    /**
     * Converts from a chat code
     * @param readIndex the index at which to start reading
     * @param code the input string
     * @return the value
     */
    @Nullable V readChatCode(@NonNull String @NonNull [] code, int readIndex);

}
