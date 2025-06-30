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

    @SafeVarargs
    public DefineOrder(OrderedField<B, ?>...orderables) {
        this.orderables = Objects.requireNonNull(orderables);
    }

    private interface Inequality {
        <F> Condition comparison(Field<F> field, F compareValue);
    }

    private <F> Condition isGreaterOrLess(OrderedField<B, F> orderedField, B borderValue,
                                          Inequality inequality) {
        Field<F> field = orderedField.field();
        F compareValue = orderedField.extractFrom(borderValue);
        return inequality.comparison(field, compareValue);
    }

    private <F> Condition isEqual(OrderedField<B, F> orderedField, B borderValue) {
        Field<F> field = orderedField.field();
        F compareValue = orderedField.extractFrom(borderValue);
        return field.eq(compareValue);
    }

    /**
     * Builds a seeking predicate using all fields. Uses the order with which this {@code DefineOrder} was created.
     * <p>
     * Pagination allows some fields to be equal, in which case, other fields break ties. For example, for a two-field
     * order based on fields <code>f1, f2</code> with border values <code>v1, v2</code>, compared in ascending order,
     * the predicate would look like this this:
     * <pre>
     *     {@code
     *       WHERE f1 > v1 OR f1 = v1 AND f2 > v2
     *     }
     * </pre>
     * If we added a field <code>f3</code> and value <code>v3</code> to the mix, it would result in an elongation of
     * the same predicate (parentheses added for clarity):
     * <pre>
     *     {@code
     *       WHERE (f1 > v1) OR (f1 = v1 AND f2 > v2) OR (f1 = v1 AND f2 = v2 AND f3 > v3)
     *     }
     * </pre>
     *
     * @param borderValue the border values for each field, packed into a single object
     * @param inequality the inequality (strictly greater or strictly less)
     * @return the predicate
     */
    private Condition fieldByFieldPredicate(B borderValue, Inequality inequality) {
        // A combination of fields from previous iterations
        Condition previousFieldsEqual = noCondition();
        Condition built = noCondition();
        for (OrderedField<B, ?> orderedField : orderables) {
            built = built.or(
                    // The current condition. Only works if previous fields (if set) are tied
                    previousFieldsEqual.and(isGreaterOrLess(orderedField, borderValue, inequality))
            );
            previousFieldsEqual = previousFieldsEqual.and(isEqual(orderedField, borderValue));
        }
        return built;
    }

    public Condition greaterThan(B borderValue) {
        return fieldByFieldPredicate(borderValue, Field::greaterThan);
    }

    public Condition lessThan(B borderValue) {
        return fieldByFieldPredicate(borderValue, Field::lessThan);
    }

    public OrderField<?>[] ascending() {
        return sortBy(SortOrder.ASC);
    }

    public OrderField<?>[] descending() {
        return sortBy(SortOrder.DESC);
    }

    private OrderField<?>[] sortBy(SortOrder sortOrder) {
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
