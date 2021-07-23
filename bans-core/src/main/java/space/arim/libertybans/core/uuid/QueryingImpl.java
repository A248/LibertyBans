/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.uuid;

import java.util.UUID;

import jakarta.inject.Provider;

import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.omnibus.util.UUIDUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.database.InternalDatabase;

class QueryingImpl {

	private final Provider<InternalDatabase> dbProvider;

	QueryingImpl(Provider<InternalDatabase> dbProvider) {
		this.dbProvider = dbProvider;
	}

	CentralisedFuture<UUID> resolve(String name) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			return database.jdbCaesar().query(
					"SELECT `uuid` FROM `libertybans_names` WHERE `name` = ? ORDER BY `updated` DESC LIMIT 1")
					.params(name)
					.singleResult((resultSet) -> UUIDUtil.fromByteArray(resultSet.getBytes("uuid")))
					.execute();
		});
	}

	CentralisedFuture<String> resolve(UUID uuid) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			return database.jdbCaesar().query(
					"SELECT `name` FROM `libertybans_names` WHERE `uuid` = ? ORDER BY `updated` DESC LIMIT 1")
					.params(uuid)
					.singleResult((resultSet) -> resultSet.getString("name"))
					.execute();
		});
	}

	/*
	 * Other lookups
	 */

	CentralisedFuture<NetworkAddress> resolveAddress(String name) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			return database.jdbCaesar().query(
					"SELECT `addrs`.`address` FROM `libertybans_addresses` `addrs` INNER JOIN `libertybans_names` `names` "
					+ "ON `addrs`.`uuid` = `names`.`uuid` WHERE `names`.`name` = ? "
					+ "ORDER BY `names`.`updated` DESC, `addrs`.`updated` DESC LIMIT 1")
					.params(name)
					.singleResult((resultSet) -> NetworkAddress.of(resultSet.getBytes("address")))
					.execute();
		});
	}

	CentralisedFuture<UUIDAndAddress> resolvePlayer(String name) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			return database.jdbCaesar().query("SELECT `uuid`, `addrs`.`address` " +
					"FROM `libertybans_names` `names` " +
					"INNER JOIN `libertybans_addresses` `addrs` " +
					"ON `names`.`uuid` = `addrs`.`uuid` " +
					"WHERE `names`.`name`")
					.params(name)
					.singleResult((resultSet) -> {
						return new UUIDAndAddress(
								UUIDUtil.fromByteArray(resultSet.getBytes("uuid")),
								NetworkAddress.of(resultSet.getBytes("address")));
					}).execute();
		});
	}

}
