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
public class PaginationMultiKeyIT {

    private static final int PER_PAGE = 4;

    private final Provider<QueryExecutor> queryExecutor;

    @Inject
    public PaginationMultiKeyIT(Provider<QueryExecutor> queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    private KeysetPage<Entry, TypeThenId> getPage(Pagination<TypeThenId> pagination) {
        return queryExecutor.get().query(SQLFunction.readOnly((context) -> {
            List<Entry> punishments = context
                    .selectFrom(PUNISHMENTS)
                    .where(pagination.seeking())
                    .orderBy(pagination.order())
                    .limit(PER_PAGE)
                    .fetch((record) ->
                            new Entry(record.getId(), record.getType(), record.getReason())
                    );
            return pagination.buildPage(punishments, new KeysetPage.AnchorLiaison<>() {
                @Override
                public BorderValueHandle<TypeThenId> borderValueHandle() {
                    return TypeThenId.borderValueHandle();
                }

                @Override
                public TypeThenId getAnchor(Entry datum) {
                    return new TypeThenId(datum.type, datum.id);
                }
            });
        })).join();
    }

    private static void assertPageData(KeysetPage<Entry, TypeThenId> page, long...ids) {
        assertArrayEquals(
                ids,
                page.data().stream().mapToLong(Entry::id).toArray(),
                "expected different page data"
        );
    }

    record Entry(long id, PunishmentType type, String reason) {}

    @TestTemplate
    public void paginatePunishments() {
        Pagination<TypeThenId> pagination = new Pagination<>(
                KeysetAnchor.unset(), true, TypeThenId.defineOrder(PUNISHMENTS.TYPE, PUNISHMENTS.ID)
        );
        var firstPage = getPage(pagination);
        assertEquals(PER_PAGE, firstPage.data().size());
        assertPageData(firstPage, 1, 2, 3, 4, 5);

        var secondPage = getPage(pagination.changeAnchor(firstPage.nextPageAnchor()));
        assertPageData(secondPage, 7, 8, 9, 10, 11);

        var thirdPage = getPage(pagination.changeAnchor(secondPage.nextPageAnchor()));
        assertPageData(thirdPage, 12, 13, 14, 15, 16);

        // Now go backwards
        var backToSecondPage = getPage(pagination.changeAnchor(thirdPage.lastPageAnchor()));
        assertEquals(secondPage, backToSecondPage, "2nd page from forward/backward navigation should be same");

        var backToFirstPage = getPage(pagination.changeAnchor(secondPage.lastPageAnchor()));
        assertEquals(firstPage, backToFirstPage, "1st page from forward/backward navigation should be same");

        KeysetPage<Entry, TypeThenId> fourPagesLater = thirdPage;
        for (int n = 0; n < 4; n++) {
            fourPagesLater = getPage(pagination.changeAnchor(fourPagesLater.nextPageAnchor()));
        }
        assertPageData(fourPagesLater, 32, 34, 35, 37, 38);
    }

    @TestTemplate
    @SampleData(source = SampleData.Source.BlueTree)
    public void paginatePunishmentsDescending() {
        Pagination<TypeThenId> pagination = new Pagination<>(
                KeysetAnchor.unset(), false, TypeThenId.defineOrder(PUNISHMENTS.TYPE, PUNISHMENTS.ID)
        );
        var firstPage = getPage(pagination);
        assertEquals(PER_PAGE, firstPage.data().size());
        assertPageData(firstPage, 262, 261, 260, 259, 258);

        var secondPage = getPage(pagination.changeAnchor(firstPage.nextPageAnchor()));
        assertPageData(secondPage, 257, 256, 255, 254, 253);

        var thirdPage = getPage(pagination.changeAnchor(secondPage.nextPageAnchor()));
        assertPageData(thirdPage, 252, 251, 250, 249, 248);

        // Now go backwards
        var backToSecondPage = getPage(pagination.changeAnchor(thirdPage.lastPageAnchor()));
        assertEquals(secondPage, backToSecondPage, "2nd page from forward/backward navigation should be same");

        var backToFirstPage = getPage(pagination.changeAnchor(secondPage.lastPageAnchor()));
        assertEquals(firstPage, backToFirstPage, "1st page from forward/backward navigation should be same");
    }
}
