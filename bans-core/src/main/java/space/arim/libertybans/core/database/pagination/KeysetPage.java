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

import java.util.List;
import java.util.Objects;

public record KeysetPage<R, F>(List<R> data,
                               KeysetAnchor<F> lastPageAnchor,
                               KeysetAnchor<F> nextPageAnchor) {

    public KeysetPage {
        Objects.requireNonNull(data);
        Objects.requireNonNull(lastPageAnchor);
        Objects.requireNonNull(nextPageAnchor);
    }

    public interface ExtractAnchor<R, F> {

        /**
         * Gets the page anchor from this data point
         * @param datum the data point
         * @return the page anchor border value
         */
        F getAnchor(R datum);
    }

}
