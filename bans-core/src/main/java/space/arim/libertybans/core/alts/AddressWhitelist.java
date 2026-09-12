/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

package space.arim.libertybans.core.alts;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.DSLContext;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.pagination.BorderValueHandle;
import space.arim.libertybans.core.database.pagination.InstantThenAddress;
import space.arim.libertybans.core.database.pagination.KeysetAnchor;
import space.arim.libertybans.core.database.pagination.KeysetPage;
import space.arim.libertybans.core.database.pagination.Pagination;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static space.arim.libertybans.core.schema.tables.AddressWhitelist.ADDRESS_WHITELIST;
import static space.arim.libertybans.core.schema.tables.LatestNames.LATEST_NAMES;

@Singleton
public final class AddressWhitelist {

    private final Provider<QueryExecutor> queryExecutor;
    private final Time time;

    private final Cache<NetworkAddress, Boolean> whitelistCache;

    @Inject
    public AddressWhitelist(Provider<QueryExecutor> queryExecutor, Time time) {
        this.queryExecutor = queryExecutor;
        this.time = time;
        whitelistCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(4L))
                .build();
    }

    public boolean isWhitelisted(DSLContext context, NetworkAddress address) {
        return whitelistCache.get(address, addr -> {
            int count = context
                    .selectCount()
                    .from(ADDRESS_WHITELIST)
                    .where(ADDRESS_WHITELIST.ADDRESS.eq(addr))
                    .fetchSingle()
                    .value1();
            return count != 0;
        });
    }

    public CentralisedFuture<Boolean> add(NetworkAddress address, Operator whitelistedBy) {
        return queryExecutor.get().queryWithRetry((context, transaction) -> {
            int count = context
                    .insertInto(ADDRESS_WHITELIST)
                    .columns(ADDRESS_WHITELIST.ADDRESS, ADDRESS_WHITELIST.WHITELISTED_ON, ADDRESS_WHITELIST.WHITELISTED_BY)
                    .values(address, time.currentTimestamp(), whitelistedBy)
                    .onConflictDoNothing()
                    .execute();
            return count != 0;
        });
    }

    public CentralisedFuture<Boolean> remove(NetworkAddress address) {
        return queryExecutor.get().queryWithRetry((context, transaction) -> {
            int count = context
                    .deleteFrom(ADDRESS_WHITELIST)
                    .where(ADDRESS_WHITELIST.ADDRESS.eq(address))
                    .execute();
            return count != 0;
        });
    }

    public record ListRequest(int pageSize, KeysetAnchor<InstantThenAddress> pageAnchor, int skipCount) {

        public ListRequest {
            Objects.requireNonNull(pageAnchor);
        }
    }

    public CentralisedFuture<KeysetPage<Entry, InstantThenAddress>> list(ListRequest request) {
        return queryExecutor.get().query(SQLFunction.readOnly(context -> {
            Pagination<InstantThenAddress> pagination = new Pagination<>(
                    request.pageAnchor, false, InstantThenAddress.defineOrder(ADDRESS_WHITELIST.WHITELISTED_ON, ADDRESS_WHITELIST.ADDRESS)
            );
            List<Entry> entries = context
                    .select(ADDRESS_WHITELIST.ADDRESS, ADDRESS_WHITELIST.WHITELISTED_BY, ADDRESS_WHITELIST.WHITELISTED_ON, LATEST_NAMES.NAME)
                    .from(ADDRESS_WHITELIST)
                    .leftJoin(LATEST_NAMES)
                    .on(LATEST_NAMES.UUID.eq(ADDRESS_WHITELIST.WHITELISTED_BY.coerce(UUID.class)))
                    .where(pagination.seeking())
                    .orderBy(pagination.order())
                    .limit(request.pageSize)
                    .offset(request.skipCount)
                    .fetch((record) ->
                            new Entry(record.value1(), record.value2(), record.value3(), record.value4()));
            return pagination.anchor().buildPage(entries, new KeysetPage.AnchorLiaison<>() {
                @Override
                public BorderValueHandle<InstantThenAddress> borderValueHandle() {
                    return InstantThenAddress.borderValueHandle();
                }

                @Override
                public InstantThenAddress getAnchor(Entry datum) {
                    return new InstantThenAddress(datum.date, datum.address);
                }
            });
        }));
    }

    public record Entry(NetworkAddress address, Operator operator, Instant date, @Nullable String operatorName) {
    }

}
