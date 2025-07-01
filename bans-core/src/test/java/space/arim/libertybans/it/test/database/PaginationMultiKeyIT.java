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
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.pagination.*;
import space.arim.libertybans.it.InjectionInvocationContextProvider;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static space.arim.libertybans.core.schema.tables.Punishments.PUNISHMENTS;

@ExtendWith(InjectionInvocationContextProvider.class)
public class PaginationMultiKeyIT {

    private static final int PER_PAGE = 3;

    private final Provider<QueryExecutor> queryExecutor;

    @Inject
    public PaginationMultiKeyIT(Provider<QueryExecutor> queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    private void writeSampleData(Entry...entries) {
        queryExecutor.get().execute(context -> {
            Instant currentTime = Instant.now();
            for (Entry entry : entries) {
                context
                        .insertInto(PUNISHMENTS)
                        .columns(
                                PUNISHMENTS.ID, PUNISHMENTS.TYPE, PUNISHMENTS.OPERATOR, PUNISHMENTS.REASON,
                                PUNISHMENTS.SCOPE, PUNISHMENTS.START, PUNISHMENTS.END,
                                PUNISHMENTS.TRACK, PUNISHMENTS.SCOPE_ID
                        )
                        .values(
                                entry.id, entry.type, ConsoleOperator.INSTANCE, entry.reason,
                                "", currentTime, Punishment.PERMANENT_END_DATE, null, null
                        )
                        .execute();
            }
        }).join();
    }

    private KeysetPage<Entry, TypeThenId> getPage(Pagination<TypeThenId> pagination) {
        return queryExecutor.get().query(SQLFunction.readOnly(context -> {
            List<Entry> punishments = context
                    .selectFrom(PUNISHMENTS)
                    .where(pagination.seeking())
                    .orderBy(pagination.order())
                    .limit(PER_PAGE)
                    .fetch((record) ->
                            new Entry(record.getId(), record.getType(), record.getReason())
                    );
            return pagination.anchor().buildPage(punishments, new KeysetPage.AnchorLiaison<>() {
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
        writeSampleData(
                new Entry(1, PunishmentType.KICK, "k1"),
                new Entry(4, PunishmentType.KICK, "k2"),
                new Entry(3, PunishmentType.BAN, "second ban"),
                new Entry(2, PunishmentType.BAN, "first ban"),
                new Entry(5, PunishmentType.BAN, "third ban"),
                new Entry(6, PunishmentType.BAN, "fourth ban"),
                new Entry(7, PunishmentType.MUTE, "first mute"),
                new Entry(8, PunishmentType.MUTE, "second mute"),
                new Entry(9, PunishmentType.WARN, "warning"),
                new Entry(10, PunishmentType.KICK, "k3")
        );
        Pagination<TypeThenId> pagination = new Pagination<>(
                KeysetAnchor.unset(), true, TypeThenId.defineOrder(PUNISHMENTS.TYPE, PUNISHMENTS.ID)
        );
        var firstPage = getPage(pagination);
        assertEquals(PER_PAGE, firstPage.data().size());
        assertPageData(firstPage, 2, 3, 5);

        var secondPage = getPage(pagination.withAnchor(firstPage.nextPageAnchor()));
        assertPageData(secondPage, 6, 7, 8);

        var thirdPage = getPage(pagination.withAnchor(secondPage.nextPageAnchor()));
        assertPageData(thirdPage, 9, 1, 4);

        var fourthPage = getPage(pagination.withAnchor(thirdPage.nextPageAnchor()));
        assertPageData(fourthPage, 10);

        // Now go backwards
        var backToThirdPage = getPage(pagination.withAnchor(fourthPage.lastPageAnchor()));
        assertEquals(thirdPage, backToThirdPage, "3rd page from forward/backward navigation should be same");

        var backToSecondPage = getPage(pagination.withAnchor(thirdPage.lastPageAnchor()));
        assertEquals(secondPage, backToSecondPage, "2nd page from forward/backward navigation should be same");

        var backToFirstPage = getPage(pagination.withAnchor(secondPage.lastPageAnchor()));
        assertEquals(firstPage, backToFirstPage, "1st page from forward/backward navigation should be same");
    }

    @TestTemplate
    public void paginatePunishmentsDescending() {
        writeSampleData(
                new Entry(1, PunishmentType.KICK, "k1"),
                new Entry(4, PunishmentType.KICK, "k2"),
                new Entry(3, PunishmentType.BAN, "second ban"),
                new Entry(2, PunishmentType.BAN, "first ban"),
                new Entry(5, PunishmentType.BAN, "third ban"),
                new Entry(6, PunishmentType.BAN, "fourth ban"),
                new Entry(7, PunishmentType.MUTE, "first mute"),
                new Entry(8, PunishmentType.MUTE, "second mute"),
                new Entry(9, PunishmentType.WARN, "warning"),
                new Entry(10, PunishmentType.KICK, "k3")
        );
        Pagination<TypeThenId> pagination = new Pagination<>(
                KeysetAnchor.unset(), false, TypeThenId.defineOrder(PUNISHMENTS.TYPE, PUNISHMENTS.ID)
        );
        var firstPage = getPage(pagination);
        assertEquals(PER_PAGE, firstPage.data().size());
        assertPageData(firstPage, 10, 4, 1);

        var secondPage = getPage(pagination.withAnchor(firstPage.nextPageAnchor()));
        assertPageData(secondPage, 9, 8, 7);

        var thirdPage = getPage(pagination.withAnchor(secondPage.nextPageAnchor()));
        assertPageData(thirdPage, 6, 5, 3);

        var fourthPage = getPage(pagination.withAnchor(thirdPage.nextPageAnchor()));
        assertPageData(fourthPage, 2);

        // Now go backwards
        var backToThirdPage = getPage(pagination.withAnchor(fourthPage.lastPageAnchor()));
        assertEquals(thirdPage, backToThirdPage, "3rd page from forward/backward navigation should be same");

        var backToSecondPage = getPage(pagination.withAnchor(thirdPage.lastPageAnchor()));
        assertEquals(secondPage, backToSecondPage, "2nd page from forward/backward navigation should be same");

        var backToFirstPage = getPage(pagination.withAnchor(secondPage.lastPageAnchor()));
        assertEquals(firstPage, backToFirstPage, "1st page from forward/backward navigation should be same");
    }
}
