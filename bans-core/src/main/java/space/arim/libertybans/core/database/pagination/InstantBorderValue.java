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

import java.time.Instant;
import java.util.Optional;

public record InstantBorderValue(LongBorderValue longBorderValue) implements BorderValueHandle<Instant> {

    public static final InstantBorderValue GET = new InstantBorderValue(new LongBorderValue());

    @Override
    public String[] chatCode(Instant value) {
        return longBorderValue.chatCode(value.getEpochSecond());
    }

    @Override
    public Optional<Instant> fromCode(int startIndex, String[] code) {
        return longBorderValue.fromCode(startIndex, code).map(Instant::ofEpochSecond);
    }
}
