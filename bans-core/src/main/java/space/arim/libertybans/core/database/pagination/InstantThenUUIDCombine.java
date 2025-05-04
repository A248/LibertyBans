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
import java.util.UUID;

public final class InstantThenUUIDCombine implements CombineValues<Instant, UUID, InstantThenUUID> {
    @Override
    public InstantThenUUID combine(Instant first, UUID second) {
        return new InstantThenUUID(first, second);
    }

    @Override
    public Instant first(InstantThenUUID combined) {
        return combined.instant();
    }

    @Override
    public UUID second(InstantThenUUID combined) {
        return combined.uuid();
    }

    public BorderValueHandle<InstantThenUUID> borderValueHandle() {

        record Handles(BorderValueHandle<Instant> handle1, BorderValueHandle<UUID> handle2)
                implements CombinedBorderValue.CombineHandles<Instant, UUID> {}

        LongBorderValue longBorderValue = new LongBorderValue();
        return new CombinedBorderValue<>(
                new Handles(
                        new InstantBorderValue(longBorderValue), new UUIDCombine().borderValueHandle(longBorderValue)
                ),
                this
        );
    }
}
