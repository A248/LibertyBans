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

package space.arim.libertybans.core.alts;

import jakarta.inject.Inject;
import space.arim.libertybans.api.*;
import space.arim.libertybans.api.user.AccountSupervisor;
import space.arim.libertybans.api.user.AltDetectionQuery;
import space.arim.libertybans.api.user.KnownAccount;
import space.arim.libertybans.core.database.pagination.KeysetAnchor;
import space.arim.libertybans.core.database.pagination.KeysetPage;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Supervisor implements AccountSupervisor {

	private final AltDetection altDetection;
	private final AccountHistory accountHistory;

	@Inject
	public Supervisor(AltDetection altDetection, AccountHistory accountHistory) {
		this.altDetection = altDetection;
		this.accountHistory = accountHistory;
	}

	@Override
	public AltDetectionQuery.Builder detectAlts(UUID uuid, NetworkAddress address) {
		class Builder implements AltDetectionQuery.Builder {

			private Set<PunishmentType> types = Set.of();

			@Override
			public AltDetectionQuery.Builder punishmentTypes(Set<PunishmentType> types) {
				this.types = Set.copyOf(types);
				return this;
			}

			@Override
			public AltDetectionQuery build() {
				return new AltDetection.AltQuery(new AltInfoRequest(uuid, address, WhichAlts.ALL_ALTS, true, Short.MAX_VALUE), types, altDetection);
			}
		}
		return new Builder();
	}

	public CentralisedFuture<List<? extends KnownAccount>> knownAccounts(Victim victim) {
		return accountHistory.knownAccounts(
				victim, new AccountHistory.Request(Short.MAX_VALUE, KeysetAnchor.unset(), 0)
		).thenApply(KeysetPage::data);
	}

	@Override
	public CentralisedFuture<List<? extends KnownAccount>> findAccountsMatching(UUID uuid) {
		return knownAccounts(PlayerVictim.of(uuid));
	}

	@Override
	public CentralisedFuture<List<? extends KnownAccount>> findAccountsMatching(NetworkAddress address) {
		return knownAccounts(AddressVictim.of(address));
	}

	@Override
	public CentralisedFuture<List<? extends KnownAccount>> findAccountsMatching(UUID uuid, NetworkAddress address) {
		return knownAccounts(CompositeVictim.of(uuid, address));
	}

}
