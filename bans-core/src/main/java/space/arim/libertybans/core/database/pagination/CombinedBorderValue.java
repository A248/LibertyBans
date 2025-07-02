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

record CombinedBorderValue<V1, V2, C>(CombineHandles<V1, V2> handles, CombineValues<V1, V2, C> combinator)
        implements BorderValueHandle<C> {

    private BorderValueHandle<V1> handle1() {
        return handles.handle1();
    }

    private BorderValueHandle<V2> handle2() {
        return handles.handle2();
    }

    @Override
    public int len() {
        return handle1().len() + handle2().len();
    }

    @Override
    public void writeChatCode(@NonNull C value, @NonNull Write write) {
        handle1().writeChatCode(combinator.first(value), write);
        handle2().writeChatCode(combinator.second(value), write);
    }

    @Override
    public @Nullable C readChatCode(@NonNull Read read) {
        V1 first = handle1().readChatCode(read);
        if (first != null) {
            V2 second = handle2().readChatCode(read);
            if (second != null) {
                return combinator.combine(first, second);
            }
        }
        return null;
    }

    interface CombineHandles<V1, V2> {

        BorderValueHandle<V1> handle1();

        BorderValueHandle<V2> handle2();

    }
}
