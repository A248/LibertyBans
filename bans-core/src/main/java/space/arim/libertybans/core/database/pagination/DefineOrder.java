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
import org.jooq.SortOrder;

import java.util.Objects;

import static org.jooq.impl.DSL.noCondition;

public final class DefineOrder<B> {

    private final OrderedField<B, ?>[] orderables;

    public DefineOrder(OrderedField<B, ?>...orderables) {
        this.orderables = Objects.requireNonNull(orderables);
    }

    private <F> Condition greaterThan(OrderedField<B, F> orderedField, B borderValue) {
        return orderedField.field().greaterThan(orderedField.extractFrom(borderValue));
    }

    public Condition greaterThan(B borderValue) {
        Condition built = noCondition();
        for (OrderedField<B, ?> orderedField : orderables) {
            built = built.and(greaterThan(orderedField, borderValue));
        }
        return built;
    }

    private <F> Condition lessThan(OrderedField<B, F> orderedField, B borderValue) {
        return orderedField.field().lessThan(orderedField.extractFrom(borderValue));
    }

    public Condition lessThan(B borderValue) {
        Condition built = noCondition();
        for (OrderedField<B, ?> orderedField : orderables) {
            built = built.and(lessThan(orderedField, borderValue));
        }
        return built;
    }

    public OrderField<?>[] ascending() {
        return inOrder(SortOrder.ASC);
    }

    public OrderField<?>[] descending() {
        return inOrder(SortOrder.DESC);
    }

    private OrderField<?>[] inOrder(SortOrder sortOrder) {
        int len = orderables.length;
        OrderField<?>[] fields = new OrderField[len];
        for (int n = 0; n < len; n++) {
            fields[n] = orderables[n].field().sort(sortOrder);
        }
        return fields;
    }

    public interface OrderedField<B, F> {

        Field<F> field();

        F extractFrom(B borderValue);

    }

    public record SimpleOrderedField<F>(Field<F> field) implements OrderedField<F, F> {

        @Override
        public F extractFrom(F borderValue) {
            return borderValue;
        }
    }
}
