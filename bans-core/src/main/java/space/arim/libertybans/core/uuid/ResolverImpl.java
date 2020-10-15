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

import java.util.Map;
import java.util.UUID;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.uuidvault.api.UUIDResolver;
import space.arim.uuidvault.api.UUIDUtil;

import space.arim.libertybans.core.database.Database;

class ResolverImpl implements UUIDResolver {

	private final UUIDManager manager;
	
	ResolverImpl(UUIDManager manager) {
		this.manager = manager;
	}
	
	@Override
	public CentralisedFuture<UUID> resolve(String name) {
		Database helper = manager.core.getDatabase();
		return helper.selectAsync(() -> {
			return helper.jdbCaesar().query(
					"SELECT `uuid` FROM `libertybans_names` WHERE `name` = ? ORDER BY `updated` DESC LIMIT 1")
					.params(name)
					.singleResult((resultSet) -> {
						return UUIDUtil.fromByteArray(resultSet.getBytes("uuid"));
					}).onError(() -> null).execute();
		});
	}

	@Override
	public CentralisedFuture<String> resolve(UUID uuid) {
		Database helper = manager.core.getDatabase();
		return helper.selectAsync(() -> {
			return helper.jdbCaesar().query(
					"SELECT `name` FROM `libertybans_names` WHERE `uuid` = ? ORDER BY `updated` DESC LIMIT 1")
					.params(uuid)
					.singleResult((resultSet) -> {
						return resultSet.getString("name");
					}).onError(() -> null).execute();
		});
	}

	@Override
	public UUID resolveImmediately(String name) {
		// Caffeine specifies that operations on the entry set do not refresh the expiration timer
		for (Map.Entry<UUID, String> entry : manager.fastCache.asMap().entrySet()) {
			if (entry.getValue().equalsIgnoreCase(name)) {
				UUID uuid = entry.getKey();
				// Manual cache refresh
				manager.fastCache.getIfPresent(uuid);
				return uuid;
			}
		}
		return null;
	}

	@Override
	public String resolveImmediately(UUID uuid) {
		return manager.fastCache.getIfPresent(uuid);
	}
	
}
