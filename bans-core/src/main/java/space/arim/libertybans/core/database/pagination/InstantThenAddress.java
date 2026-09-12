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
import space.arim.libertybans.api.NetworkAddress;

import java.time.Instant;
import java.util.Objects;

public record InstantThenAddress(Instant instant, NetworkAddress address) {

    public InstantThenAddress {
        Objects.requireNonNull(instant);
        Objects.requireNonNull(address);
    }

    public static DefineOrder<InstantThenAddress> defineOrder(Field<Instant> instantField, Field<NetworkAddress> addressField) {
        return new DefineOrder<>(
                new DefineOrder.OrderedField<InstantThenAddress, Instant>() {
                    @Override
                    public Field<Instant> field() {
                        return instantField;
                    }

                    @Override
                    public Instant extractFrom(InstantThenAddress borderValue) {
                        return borderValue.instant;
                    }
                },
                new DefineOrder.OrderedField<InstantThenAddress, NetworkAddress>() {
                    @Override
                    public Field<NetworkAddress> field() {
                        return addressField;
                    }

                    @Override
                    public NetworkAddress extractFrom(InstantThenAddress borderValue) {
                        return borderValue.address;
                    }
                }
        );
    }

    public static BorderValueHandle<InstantThenAddress> borderValueHandle() {
        record Handles(BorderValueHandle<Instant> handle1, BorderValueHandle<NetworkAddress> handle2)
                implements CombinedBorderValue.CombineHandles<Instant, NetworkAddress> {}

        LongBorderValue longBorderValue = new LongBorderValue();
        return new CombinedBorderValue<>(
                new Handles(
                        new InstantBorderValue(longBorderValue), new AddressBorderValue()
                ),
                new CombineValues<>() {
                    @Override
                    public InstantThenAddress combine(Instant first, NetworkAddress second) {
                        return new InstantThenAddress(first, second);
                    }

                    @Override
                    public Instant first(InstantThenAddress combined) {
                        return combined.instant;
                    }

                    @Override
                    public NetworkAddress second(InstantThenAddress combined) {
                        return combined.address;
                    }
                }
        );
    }
}
