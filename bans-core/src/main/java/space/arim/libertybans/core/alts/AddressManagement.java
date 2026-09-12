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

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.DSLContext;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.selector.EnforcementConfig;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.time.Instant;

import static space.arim.libertybans.core.schema.tables.Addresses.ADDRESSES;

@Singleton
public final class AddressManagement {

    private final Provider<QueryExecutor> queryExecutor;

    @Inject
    public AddressManagement(Provider<QueryExecutor> queryExecutor) {
        this.queryExecutor = queryExecutor;
    }

    public @Nullable Component hasExceededLimit(DSLContext context, EnforcementConfig enforcementConfig,
                                                NetworkAddress address, Instant currentTime) {
        var config = enforcementConfig.connectionLimiter();
        if (config.enable()) {
            Instant timeBeforeDuration = currentTime.minusSeconds(config.durationSeconds());
            int count = context
                    .selectCount()
                    .from(ADDRESSES)
                    .where(ADDRESSES.ADDRESS.eq(address))
                    .and(ADDRESSES.UPDATED.greaterOrEqual(timeBeforeDuration))
                    .fetchSingle()
                    .value1();
            if (count > config.limit()) {
                return config.message();
            }
        }
        return null;
    }

    public CentralisedFuture<Integer> purgeRecords(NetworkAddress address) {
        return queryExecutor.get().queryWithRetry((context, transaction) -> context
                .deleteFrom(ADDRESSES)
                .where(ADDRESSES.ADDRESS.eq(address))
                .execute());
    }
}
