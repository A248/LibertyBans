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

import java.util.UUID;

final class UUIDCombine implements CombineValues<Long, Long, UUID> {

    @Override
    public UUID combine(Long first, Long second) {
        return new UUID(first, second);
    }

    @Override
    public Long first(UUID combined) {
        return combined.getMostSignificantBits();
    }

    @Override
    public Long second(UUID combined) {
        return combined.getLeastSignificantBits();
    }

    BorderValueHandle<UUID> borderValueHandle(BorderValueHandle<Long> bitHandle) {
        class Handles implements CombinedBorderValue.CombineHandles<Long, Long> {

            @Override
            public BorderValueHandle<Long> handle1() {
                return bitHandle;
            }

            @Override
            public BorderValueHandle<Long> handle2() {
                return bitHandle;
            }
        }
        return new CombinedBorderValue<>(new Handles(), this);
    }
}
