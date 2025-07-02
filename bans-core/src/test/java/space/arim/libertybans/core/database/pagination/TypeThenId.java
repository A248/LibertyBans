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
import org.jooq.Field;
import space.arim.libertybans.api.PunishmentType;

public record TypeThenId(PunishmentType type, long id) {

    public static DefineOrder<TypeThenId> defineOrder(Field<PunishmentType> typeField, Field<Long> idField) {
        return new DefineOrder<>(
                new DefineOrder.OrderedField<TypeThenId, PunishmentType>() {
                    @Override
                    public Field<PunishmentType> field() {
                        return typeField;
                    }

                    @Override
                    public PunishmentType extractFrom(TypeThenId borderValue) {
                        return borderValue.type;
                    }
                },
                new DefineOrder.OrderedField<TypeThenId, Long>() {
                    @Override
                    public Field<Long> field() {
                        return idField;
                    }

                    @Override
                    public Long extractFrom(TypeThenId borderValue) {
                        return borderValue.id;
                    }
                }
        );
    }

    public static BorderValueHandle<TypeThenId> borderValueHandle() {
        record Handles(BorderValueHandle<PunishmentType> handle1, BorderValueHandle<Long> handle2)
                implements CombinedBorderValue.CombineHandles<PunishmentType, Long> {}

        LongBorderValue longBorderValue = new LongBorderValue();
        return new CombinedBorderValue<>(
                new Handles(new BorderValueHandle<>() {
                    @Override
                    public int len() {
                        return 1;
                    }

                    @Override
                    public void writeChatCode(@NonNull PunishmentType value, @NonNull Write write) {
                        write.writePart(value.name());
                    }

                    @Override
                    public @Nullable PunishmentType readChatCode(@NonNull Read read) {
                        try {
                            return PunishmentType.valueOf(read.readPart());
                        } catch (IllegalArgumentException ignored) {
                            return null;
                        }
                    }
                }, longBorderValue),
                new CombineValues<>() {
                    @Override
                    public TypeThenId combine(PunishmentType first, Long second) {
                        return new TypeThenId(first, second);
                    }

                    @Override
                    public PunishmentType first(TypeThenId combined) {
                        return combined.type;
                    }

                    @Override
                    public Long second(TypeThenId combined) {
                        return combined.id;
                    }
                }
        );
    }
}
