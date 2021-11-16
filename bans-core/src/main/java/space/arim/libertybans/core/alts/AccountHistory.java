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
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.omnibus.util.UUIDUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AccountHistory {

	private final Provider<InternalDatabase> dbProvider;

	@Inject
	public AccountHistory(Provider<InternalDatabase> dbProvider) {
		this.dbProvider = dbProvider;
	}

	private CentralisedFuture<List<KnownAccount>> knownAccountsWhere(String columnName, Object parameter) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			return database.jdbCaesar()
					.query("SELECT " +
							"`addresses`.`uuid`, `addresses`.`address`, `latest_names`.`name`, `addresses`.`updated` " +
							"FROM `libertybans_addresses` `addresses` " +
							"INNER JOIN `libertybans_latest_names` `latest_names` " +
							"ON `addresses`.`uuid` = `latest_names`.`uuid` " +
							"WHERE `addresses`.`" + columnName + "` = ? " +
							"ORDER BY `addresses`.`updated` ASC")
					.params(parameter)
					.listResult((resultSet) -> {
						return new KnownAccount(
								UUIDUtil.fromByteArray(resultSet.getBytes("uuid")),
								resultSet.getString("name"),
								NetworkAddress.of(resultSet.getBytes("address")),
								Instant.ofEpochSecond(resultSet.getLong("updated")));
					}).execute();
		});
	}

	/**
	 * Selects known accounts for a UUID or IP address. <br>
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
			return knownAccountsWhere("uuid", uuid);
		case ADDRESS:
			NetworkAddress address = ((AddressVictim) victim).getAddress();
			return knownAccountsWhere("address", address);
		default:
			throw MiscUtil.unknownVictimType(victim.getType());
		}
	}

	public CentralisedFuture<Boolean> deleteAccount(UUID user, Instant recorded) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			int updateCount = database.jdbCaesar()
					.query("DELETE FROM `libertybans_addresses` " +
							"WHERE `uuid` = ? AND `updated` = ?")
					.params(user, recorded.getEpochSecond())
					.updateCount().execute();
			return updateCount != 0;
		});
	}
}
