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
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.UUIDUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class AltDetection {

	private final Provider<InternalDatabase> dbProvider;
	private final Time time;

	@Inject
	public AltDetection(Provider<InternalDatabase> dbProvider, Time time) {
		this.dbProvider = dbProvider;
		this.time = time;
	}

	/**
	 * Detects alts for the given account. <br>
	 * <br>
	 * The returned alts are sorted with the oldest first. This sort order contrasts with that of
	 * selecting punishments such as on the banlist, where punishments are sorted by newest first;
	 * this done because the banlist and similar displays are paginated, whereas alt detection
	 * and account history are not paginated. In the case of pagination we want the new punishments
	 * to be readily visible rather than re-showing punishments from the dawn of time, whereas in
	 * the lack of pagination we want to show a short and linear progression from old to new.
	 *
	 * @param querySource the query source with which to contact the database
	 * @param uuid the user's UUID
	 * @param address the user's address
	 * @param whichAlts which alts to detect
	 * @return the detected alts, sorted in order of oldest first
	 */
	public List<DetectedAlt> detectAlts(QuerySource<?> querySource, UUID uuid, NetworkAddress address,
										WhichAlts whichAlts) {
		// This implementation relies on strict detection including normal detection
		// The detection kind is inferred while processing the results
		long currentTime = time.currentTime();
		List<DetectedAlt> detectedAlts = querySource.query(
				"SELECT " +
				"`detected_alt`.`address` `address`, `detected_alt`.`uuid` `uuid`, " +
				"`latest_names`.`name` `name`, `detected_alt`.`updated` `updated`, " +
				"`bans`.`victim` IS NOT NULL `has_ban`, `mutes`.`victim` IS NOT NULL `has_mute` " +
				"FROM `libertybans_addresses` `addresses` " +
				// Detect alts
				"INNER JOIN `libertybans_addresses` `detected_alt` " +
				"ON `addresses`.`address` = `detected_alt`.`address` AND `addresses`.`uuid` != `detected_alt`.`uuid` " +
				// Map to names
				"INNER JOIN `libertybans_latest_names` `latest_names` " +
				"ON `latest_names`.`uuid` = `detected_alt`.`uuid` " +
				// Pair with bans
				"LEFT JOIN `libertybans_simple_bans` `bans` " +
				"ON `bans`.`victim_type` = 'PLAYER' AND `bans`.`victim` = `detected_alt`.`uuid` AND (`bans`.`end` = 0 OR `bans`.`end` > ?) " +
				// Pair with mutes
				"LEFT JOIN `libertybans_simple_mutes` `mutes` " +
				"ON `mutes`.`victim_type` = 'PLAYER' AND `mutes`.`victim` = `detected_alt`.`uuid` AND (`mutes`.`end` = 0 OR `mutes`.`end` > ?) " +
				// Select alts for the player in question
				"WHERE `addresses`.`uuid` = ? " +
				// Order with oldest first
				"ORDER BY `updated` ASC")
				.params(currentTime, currentTime, uuid)
				.listResult((resultSet) -> {
					NetworkAddress detectedAddress = NetworkAddress.of(resultSet.getBytes("address"));
					// If this alt can be detected 'normally', then the address will be the same
					DetectionKind detectionKind = (address.equals(detectedAddress)) ? DetectionKind.NORMAL : DetectionKind.STRICT;
					// Determine most significant punishment
					PunishmentType punishmentType;
					if (resultSet.getBoolean("has_ban")) {
						punishmentType = PunishmentType.BAN;
					} else if (resultSet.getBoolean("has_mute")) {
						punishmentType = PunishmentType.MUTE;
					} else {
						punishmentType = null;
					}
					return new DetectedAlt(
							detectionKind,
							punishmentType, detectedAddress,
							UUIDUtil.fromByteArray(resultSet.getBytes("uuid")),
							resultSet.getString("name"),
							Instant.ofEpochSecond(resultSet.getLong("updated")));
				}).execute();
		switch (whichAlts) {
		case ALL_ALTS:
			break;
		case BANNED_OR_MUTED_ALTS:
			detectedAlts.removeIf((detectedAlt) -> detectedAlt.punishmentType().isEmpty());
			break;
		case BANNED_ALTS:
			detectedAlts.removeIf((detectedAlt) -> detectedAlt.punishmentType().orElse(null) != PunishmentType.BAN);
			break;
		default:
			throw new IllegalArgumentException("Unknown WhichAlts " + whichAlts);
		}
		return detectedAlts;
	}

	public CentralisedFuture<List<DetectedAlt>> detectAlts(UUID uuid, NetworkAddress address, WhichAlts whichAlts) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> detectAlts(database.jdbCaesar(), uuid, address, whichAlts));
	}

	public CentralisedFuture<List<DetectedAlt>> detectAlts(UUIDAndAddress userDetails, WhichAlts whichAlts) {
		return detectAlts(userDetails.uuid(), userDetails.address(), whichAlts);
	}

}
