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
import space.arim.jdbcaesar.QuerySource;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.omnibus.util.UUIDUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AltDetection {

	private final Provider<InternalDatabase> dbProvider;

	@Inject
	public AltDetection(Provider<InternalDatabase> dbProvider) {
		this.dbProvider = dbProvider;
	}

	public List<DetectedAlt> detectAlts(QuerySource<?> querySource, UUID uuid, NetworkAddress address) {
		// This implementation relies on strict detection including normal detection
		// The detection kind is inferred while processing the results
		return querySource.query("SELECT " +
				"`detected_alt`.`address` `address`, `detected_alt`.`uuid` `uuid`, `names`.`name` `name`, `detected_alt`.`updated` `updated` " +
				"FROM `libertybans_addresses` `addresses` " +
				"INNER JOIN `libertybans_addresses` `detected_alt` " +
				"ON `addresses`.`address` = `detected_alt`.`address` AND `addresses`.`uuid` != `detected_alt`.`uuid` " +
				"INNER JOIN `libertybans_names` `names` " +
				"ON `names`.`uuid` = `detected_alt`.`uuid` AND `names`.`updated` = `detected_alt`.`updated` " +
				"WHERE `addresses`.`uuid` = ? " +
				"ORDER BY `updated` DESC")
				.params(uuid)
				.listResult((resultSet) -> {
					NetworkAddress detectedAddress = NetworkAddress.of(resultSet.getBytes("address"));
					// If this alt can be detected 'normally', then the address will be the same
					DetectionKind detectionKind = (address.equals(detectedAddress)) ? DetectionKind.NORMAL : DetectionKind.STRICT;
					return new DetectedAlt(
							detectionKind,
							detectedAddress,
							UUIDUtil.fromByteArray(resultSet.getBytes("uuid")),
							resultSet.getString("name"),
							Instant.ofEpochSecond(resultSet.getLong("updated")));
				}).execute();
	}

	public CentralisedFuture<List<DetectedAlt>> detectAlts(UUID uuid, NetworkAddress address) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> detectAlts(database.jdbCaesar(), uuid, address));
	}

	public CentralisedFuture<List<DetectedAlt>> detectAlts(UUIDAndAddress userDetails) {
		return detectAlts(userDetails.uuid(), userDetails.address());
	}

}
