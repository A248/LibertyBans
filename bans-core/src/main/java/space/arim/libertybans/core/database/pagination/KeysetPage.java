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
import java.util.function.UnaryOperator;

public final class KeysetPage<R, F> {

    private final List<R> data;
    private final KeysetAnchor<F> lastPageAnchor;
    private final KeysetAnchor<F> nextPageAnchor;

    private transient final BorderValueHandle<F> borderValueHandle;

    public KeysetPage(List<R> data, KeysetAnchor<F> lastPageAnchor, KeysetAnchor<F> nextPageAnchor,
                      BorderValueHandle<F> borderValueHandle) {
        this.data = List.copyOf(data);
        this.lastPageAnchor = Objects.requireNonNull(lastPageAnchor);
        this.nextPageAnchor = Objects.requireNonNull(nextPageAnchor);
        this.borderValueHandle = borderValueHandle;
    }

    public List<R> data() {
        return data;
    }

    public KeysetAnchor<F> lastPageAnchor() {
        return lastPageAnchor;
    }

    public KeysetAnchor<F> nextPageAnchor() {
        return nextPageAnchor;
    }

    public final class VariableReplacer implements UnaryOperator<String> {

        private final String page;
        private final String nextPage;
        private final String nextPageKey;
        private final String lastPage;
        private final String lastPageKey;

        public VariableReplacer(int page) {
            this.page = Integer.toString(page);
            this.nextPage = Integer.toString(nextPageAnchor.page());
            this.nextPageKey = nextPageAnchor.chatCode(borderValueHandle);
            this.lastPage = Integer.toString(lastPageAnchor.page());
            this.lastPageKey = lastPageAnchor.chatCode(borderValueHandle);
        }

        @Override
        public String apply(String s) {
            return s.replace("%PAGE%", page)
                    .replace("%NEXTPAGE%", nextPage)
                    .replace("%NEXTPAGE_KEY%", nextPageKey)
                    .replace("%LASTPAGE%", lastPage)
                    .replace("%LASTPAGE_KEY%", lastPageKey);
        }
    }

    public interface AnchorLiaison<R, F> {

        /**
         * The border value handle for getting chat codes from {@code F}
         *
         * @return the border value handle
         */
        BorderValueHandle<F> borderValueHandle();

        /**
         * Gets the page anchor from this data point
         * @param datum the data point
         * @return the page anchor border value
         */
        F getAnchor(R datum);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof KeysetPage<?, ?> that)) return false;

        return data.equals(that.data) && lastPageAnchor.equals(that.lastPageAnchor) && nextPageAnchor.equals(that.nextPageAnchor);
    }

    @Override
    public int hashCode() {
        int result = data.hashCode();
        result = 31 * result + lastPageAnchor.hashCode();
        result = 31 * result + nextPageAnchor.hashCode();
        return result;
    }
}
