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

import org.jooq.Field;

import java.time.Instant;
import java.util.Objects;

public record StartTimeThenId(Instant start, Long id) {

    public StartTimeThenId {
        Objects.requireNonNull(start);
        Objects.requireNonNull(id);
    }

    public static DefineOrder<StartTimeThenId> defineOrder(Field<Instant> start, Field<Long> id) {
        return new DefineOrder<>(
                new DefineOrder.OrderedField<StartTimeThenId, Instant>() {
                    @Override
                    public Field<Instant> field() {
                        return start;
                    }

                    @Override
                    public Instant extractFrom(StartTimeThenId borderValue) {
                        return borderValue.start;
                    }
                },
                new DefineOrder.OrderedField<StartTimeThenId, Long>() {
                    @Override
                    public Field<Long> field() {
                        return id;
                    }

                    @Override
                    public Long extractFrom(StartTimeThenId borderValue) {
                        return borderValue.id;
                    }
                }
        );
    }

    public static BorderValueHandle<StartTimeThenId> borderValueHandle() {
        record Handles(BorderValueHandle<Instant> handle1, BorderValueHandle<Long> handle2)
                implements CombinedBorderValue.CombineHandles<Instant, Long> {}

        LongBorderValue longBorderValue = new LongBorderValue();
        return new CombinedBorderValue<>(
                new Handles(new InstantBorderValue(longBorderValue), longBorderValue),
                new CombineValues<>() {
                    @Override
                    public StartTimeThenId combine(Instant first, Long second) {
                        return new StartTimeThenId(first, second);
                    }

                    @Override
                    public Instant first(StartTimeThenId combined) {
                        return combined.start;
                    }

                    @Override
                    public Long second(StartTimeThenId combined) {
                        return combined.id;
                    }
                }
        );
    }
}
