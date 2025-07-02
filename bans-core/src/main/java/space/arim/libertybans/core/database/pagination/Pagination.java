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
import org.jooq.OrderField;

import java.util.Objects;

import static org.jooq.impl.DSL.noCondition;

public final class Pagination<F> {

    private final KeysetAnchor<F> anchor;
    private final boolean sortAscending;
    private final DefineOrder<F> defineOrder;

    public Pagination(KeysetAnchor<F> anchor, boolean sortAscending, DefineOrder<F> defineOrder) {
        this.anchor = Objects.requireNonNull(anchor, "anchor");
        this.sortAscending = sortAscending;
        this.defineOrder = Objects.requireNonNull(defineOrder, "defineOrder");
    }

    public KeysetAnchor<F> anchor() {
        return anchor;
    }

    public Pagination<F> withAnchor(KeysetAnchor<F> newAnchor) {
        return new Pagination<>(newAnchor, sortAscending, defineOrder);
    }

    public Condition seeking() {
        if (anchor.borderValue() == null) {
            return noCondition();
        }
        //
        // Sort | Scrolling | How we need to seek
        //
        // Asc  | Forward   | Greater
        // Asc  | Backward  | Less
        // Desc | Forward   | Less
        // Desc | Backward  | Greater
        return sortAscending == anchor.fromForwardScroll() ?
                defineOrder.greaterThan(anchor.borderValue()) : defineOrder.lessThan(anchor.borderValue());
    }

    public OrderField<?>[] order() {
        // If there is no border value, we're necessarily listing forward
        boolean forwardScroll = anchor.fromForwardScroll() || anchor.borderValue() == null;
        //
        // Sort | Scrolling | What we need to show up first
        //
        // Asc  | Forward   | Least
        // Asc  | Backward  | Greatest + reverse order of results
        // Desc | Forward   | Greatest
        // Desc | Backward  | Least + reverse order of results
        return sortAscending == forwardScroll ?
                defineOrder.ascending() : defineOrder.descending();
    }

}
