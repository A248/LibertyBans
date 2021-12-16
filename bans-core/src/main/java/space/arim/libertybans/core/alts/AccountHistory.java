/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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
import org.jooq.Field;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static space.arim.libertybans.core.schema.tables.Addresses.ADDRESSES;
import static space.arim.libertybans.core.schema.tables.LatestNames.LATEST_NAMES;

public class AccountHistory {

	private final Provider<InternalDatabase> dbProvider;

	@Inject
	public AccountHistory(Provider<InternalDatabase> dbProvider) {
		this.dbProvider = dbProvider;
	}

	private <P> CentralisedFuture<List<KnownAccount>> knownAccountsWhere(Field<P> field, P parameter) {
		InternalDatabase database = dbProvider.get();
		return database.query(SQLFunction.readOnly((context) -> {
			return context
					.select(ADDRESSES.UUID, ADDRESSES.ADDRESS, LATEST_NAMES.NAME, ADDRESSES.UPDATED)
					.from(ADDRESSES)
					.innerJoin(LATEST_NAMES)
					.on(ADDRESSES.UUID.eq(LATEST_NAMES.UUID))
					.where(field.eq(parameter))
					.orderBy(ADDRESSES.UPDATED.asc())
					.fetch((record) -> {
						return new KnownAccount(
								record.get(ADDRESSES.UUID),
								record.get(LATEST_NAMES.NAME),
								record.get(ADDRESSES.ADDRESS),
								record.get(ADDRESSES.UPDATED)
						);
					});
		}));
	}

	/**
	 * Selects known accounts for a uuidField or IP address. <br>
	 * <br>
	 * The returned accounts are sorted with the oldest first. See {@link AltDetection}
	 * for a description of why this sort order is used.
	 *
	 * @param victim the uuid or IP address
	 * @return the detected alts, sorted in order of oldest first
	 */
	public CentralisedFuture<List<KnownAccount>> knownAccounts(Victim victim) {
		switch (victim.getType()) {
		case PLAYER:
			UUID uuid = ((PlayerVictim) victim).getUUID();
			return knownAccountsWhere(ADDRESSES.UUID, uuid);
		case ADDRESS:
			NetworkAddress address = ((AddressVictim) victim).getAddress();
			return knownAccountsWhere(ADDRESSES.ADDRESS, address);
		default:
			throw MiscUtil.unknownVictimType(victim.getType());
		}
	}

	public CentralisedFuture<Boolean> deleteAccount(UUID user, Instant recorded) {
		InternalDatabase database = dbProvider.get();
		return database.queryWithRetry((context, transaction) -> {
			int updateCount = context
					.deleteFrom(ADDRESSES)
					.where(ADDRESSES.UUID.eq(user))
					.and(ADDRESSES.UPDATED.eq(recorded))
					.execute();
			return updateCount != 0;
		});
	}
}
