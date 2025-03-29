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

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.OrderField;

public interface Orderable<F> {

    Condition greaterThan(F borderValue);

    Condition lessThan(F borderValue);

    OrderField<?>[] ascending();

    OrderField<?>[] descending();

    record SimpleField<F>(Field<F> field) implements Orderable<F> {

        @Override
        public Condition greaterThan(F borderValue) {
            return field.greaterThan(borderValue);
        }

        @Override
        public Condition lessThan(F borderValue) {
            return field.lessThan(borderValue);
        }

        @Override
        public OrderField<?>[] ascending() {
            return new OrderField[] {field.asc()};
        }

        @Override
        public OrderField<?>[] descending() {
            return new OrderField[] {field.desc()};
        }
    }
}
