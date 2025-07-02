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

package space.arim.libertybans.it.test.database;


import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.pagination.*;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SampleData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static space.arim.libertybans.core.schema.tables.Punishments.PUNISHMENTS;

@ExtendWith(InjectionInvocationContextProvider.class)
public class PaginationIT {

    private static final int PER_PAGE = 5;

    private final Provider<QueryExecutor> queryExecutor;

    @Inject
    public PaginationIT(Provider<QueryExecutor> queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    private KeysetPage<Entry, Long> getPage(Pagination<Long> pagination) {
        return queryExecutor.get().query(SQLFunction.readOnly((context) -> {
            List<Entry> punishments = context
                    .selectFrom(PUNISHMENTS)
                    .where(pagination.seeking())
                    .orderBy(pagination.order())
                    .limit(PER_PAGE)
                    .fetch((record) ->
                            new Entry(record.getId(), record.getType(), record.getOperator(), record.getReason())
                    );
            return pagination.anchor().buildPage(punishments, new KeysetPage.AnchorLiaison<>() {
                @Override
                public BorderValueHandle<Long> borderValueHandle() {
                    return new LongBorderValue();
                }

                @Override
                public Long getAnchor(Entry datum) {
                    return datum.id;
                }
            });
        })).join();
    }

    private static void assertPageData(KeysetPage<Entry, Long> page, long...ids) {
        assertArrayEquals(
                ids,
                page.data().stream().mapToLong(Entry::id).toArray(),
                "expected different page data"
        );
    }

    record Entry(long id, PunishmentType type, Operator operator, String reason) {}

    @TestTemplate
    @SampleData(source = SampleData.Source.BlueTree)
    public void paginatePunishments() {
        Pagination<Long> pagination = new Pagination<>(
                KeysetAnchor.unset(), true, new DefineOrder<>(new DefineOrder.SimpleOrderedField<>(PUNISHMENTS.ID))
        );
        var firstPage = getPage(pagination);
        assertEquals(PER_PAGE, firstPage.data().size());
        assertPageData(firstPage, 1, 2, 3, 4, 5);

        var secondPage = getPage(pagination.withAnchor(firstPage.nextPageAnchor()));
        assertPageData(secondPage, 7, 8, 9, 10, 11);

        var thirdPage = getPage(pagination.withAnchor(secondPage.nextPageAnchor()));
        assertPageData(thirdPage, 12, 13, 14, 15, 16);

        // Now go backwards
        var backToSecondPage = getPage(pagination.withAnchor(thirdPage.lastPageAnchor()));
        assertEquals(secondPage, backToSecondPage, "2nd page from forward/backward navigation should be same");

        var backToFirstPage = getPage(pagination.withAnchor(secondPage.lastPageAnchor()));
        assertEquals(firstPage, backToFirstPage, "1st page from forward/backward navigation should be same");

        KeysetPage<Entry, Long> fourPagesLater = thirdPage;
        for (int n = 0; n < 4; n++) {
            fourPagesLater = getPage(pagination.withAnchor(fourPagesLater.nextPageAnchor()));
        }
        assertPageData(fourPagesLater, 32, 34, 35, 37, 38);
    }

    @TestTemplate
    @SampleData(source = SampleData.Source.BlueTree)
    public void paginatePunishmentsDescending() {
        Pagination<Long> pagination = new Pagination<>(
                KeysetAnchor.unset(), false, new DefineOrder<>(new DefineOrder.SimpleOrderedField<>(PUNISHMENTS.ID))
        );
        var firstPage = getPage(pagination);
        assertEquals(PER_PAGE, firstPage.data().size());
        assertPageData(firstPage, 262, 261, 260, 259, 258);

        var secondPage = getPage(pagination.withAnchor(firstPage.nextPageAnchor()));
        assertPageData(secondPage, 257, 256, 255, 254, 253);

        var thirdPage = getPage(pagination.withAnchor(secondPage.nextPageAnchor()));
        assertPageData(thirdPage, 252, 251, 250, 249, 248);

        // Now go backwards
        var backToSecondPage = getPage(pagination.withAnchor(thirdPage.lastPageAnchor()));
        assertEquals(secondPage, backToSecondPage, "2nd page from forward/backward navigation should be same");

        var backToFirstPage = getPage(pagination.withAnchor(secondPage.lastPageAnchor()));
        assertEquals(firstPage, backToFirstPage, "1st page from forward/backward navigation should be same");
    }

}
