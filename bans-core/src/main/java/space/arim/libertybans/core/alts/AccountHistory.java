/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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
import org.jooq.Condition;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.user.KnownAccount;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static space.arim.libertybans.core.schema.tables.Addresses.ADDRESSES;
import static space.arim.libertybans.core.schema.tables.LatestNames.LATEST_NAMES;

public final class AccountHistory {

	private final Provider<QueryExecutor> queryExecutor;

	@Inject
	public AccountHistory(Provider<QueryExecutor> queryExecutor) {
		this.queryExecutor = queryExecutor;
	}

	public KnownAccount newAccount(UUID uuid, String username, NetworkAddress address, Instant recorded) {
		return new KnownAccountImpl(uuid, username, address, recorded, this);
	}

	private CentralisedFuture<List<? extends KnownAccount>> knownAccountsWhere(Condition condition) {
		return queryExecutor.get().query(SQLFunction.readOnly((context) -> {
			return context
					.select(ADDRESSES.UUID, ADDRESSES.ADDRESS, LATEST_NAMES.NAME, ADDRESSES.UPDATED)
					.from(ADDRESSES)
					.leftJoin(LATEST_NAMES)
					.on(ADDRESSES.UUID.eq(LATEST_NAMES.UUID))
					.where(condition)
					.orderBy(ADDRESSES.UPDATED.asc())
					.fetch((record) -> {
						return new KnownAccountImpl(
								record.get(ADDRESSES.UUID),
								record.get(LATEST_NAMES.NAME),
								record.get(ADDRESSES.ADDRESS),
								record.get(ADDRESSES.UPDATED),
								this
						);
					});
		}));
	}

	/**
	 * Selects known accounts for a uuid or IP address. <br>
	 * <br>
	 * The returned accounts are sorted with the oldest first. See {@link AltDetection}
	 * for a description of why this sort order is used.
	 *
	 * @param victim the uuid or IP address
	 * @return the detected alts, sorted in order of oldest first
	 */
	public CentralisedFuture<List<? extends KnownAccount>> knownAccounts(Victim victim) {
		if (victim instanceof PlayerVictim playerVictim) {
			return knownAccountsWhere(ADDRESSES.UUID.eq(playerVictim.getUUID()));

		} else if (victim instanceof AddressVictim addressVictim) {
			return knownAccountsWhere(ADDRESSES.ADDRESS.eq(addressVictim.getAddress()));

		} else if (victim instanceof CompositeVictim compositeVictim) {
			return knownAccountsWhere(ADDRESSES.UUID.eq(compositeVictim.getUUID())
					.or(ADDRESSES.ADDRESS.eq(compositeVictim.getAddress())));

		} else {
			throw MiscUtil.unknownVictimType(victim.getType());
		}
	}

	public CentralisedFuture<Boolean> deleteAccount(UUID user, Instant recorded) {
		return queryExecutor.get().queryWithRetry((context, transaction) -> {
			int updateCount = context
					.deleteFrom(ADDRESSES)
					.where(ADDRESSES.UUID.eq(user))
					.and(ADDRESSES.UPDATED.eq(recorded))
					.execute();
			return updateCount != 0;
		});
	}

	// Uses equals and hashCode for AccountHistory; see below
	record KnownAccountImpl(UUID uuid, String username, NetworkAddress address, Instant recorded,
							AccountHistory accountHistory) implements KnownAccount {

		@Override
		public Optional<String> latestUsername() {
			return Optional.ofNullable(username);
		}

		@Override
		public CentralisedFuture<Boolean> deleteFromHistory() {
			return accountHistory.deleteAccount(uuid, recorded);
		}

	}

	// Help out implementing equals and hashCode for KnownAccountImpl
	@Override
	public boolean equals(Object o) {
		return o instanceof AccountHistory;
	}

	@Override
	public int hashCode() {
		return AccountHistory.class.hashCode();
	}

}
