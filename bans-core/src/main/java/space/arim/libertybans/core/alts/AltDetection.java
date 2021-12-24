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
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.sql.EndTimeCondition;
import space.arim.libertybans.core.database.sql.SimpleViewFields;
import space.arim.libertybans.core.database.sql.VictimCondition;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static space.arim.libertybans.core.schema.tables.Addresses.ADDRESSES;
import static space.arim.libertybans.core.schema.tables.LatestNames.LATEST_NAMES;
import static space.arim.libertybans.core.schema.tables.SimpleBans.SIMPLE_BANS;
import static space.arim.libertybans.core.schema.tables.SimpleMutes.SIMPLE_MUTES;

public class AltDetection {

	private final Provider<QueryExecutor> queryExecutor;
	private final Time time;

	@Inject
	public AltDetection(Provider<QueryExecutor> queryExecutor, Time time) {
		this.queryExecutor = queryExecutor;
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
	 * @param context the query source with which to contact the database
	 * @param uuid the user's uuidField
	 * @param address the user's address
	 * @param whichAlts which alts to detect
	 * @return the detected alts, sorted in order of oldest first
	 */
	public List<DetectedAlt> detectAlts(DSLContext context, UUID uuid, NetworkAddress address,
										WhichAlts whichAlts) {
		// This implementation relies on strict detection including normal detection
		// The detection kind is inferred while processing the results
		final Instant currentTime = time.currentTimestamp();
		var detectedAlt = ADDRESSES.as("detected_alt");
		Field<Boolean> hasBan = DSL.field(SIMPLE_BANS.VICTIM_TYPE.isNotNull()).as("has_ban");
		Field<Boolean> hasMute = DSL.field(SIMPLE_MUTES.VICTIM_TYPE.isNotNull()).as("has_mute");
		List<DetectedAlt> detectedAlts = context
				.select(
						detectedAlt.ADDRESS, detectedAlt.UUID,
						LATEST_NAMES.NAME, detectedAlt.UPDATED,
						hasBan, hasMute
				)
				.from(ADDRESSES)
				// Detect alts
				.innerJoin(detectedAlt)
				.on(ADDRESSES.ADDRESS.eq(detectedAlt.ADDRESS))
				.and(ADDRESSES.UUID.notEqual(detectedAlt.UUID))
				// Map to names
				.innerJoin(LATEST_NAMES)
				.on(LATEST_NAMES.UUID.eq(detectedAlt.UUID))
				// Pair with bans
				.leftJoin(SIMPLE_BANS)
				.on(new VictimCondition(new SimpleViewFields(SIMPLE_BANS)).matchesUUID(detectedAlt.UUID))
				.and(new EndTimeCondition(new SimpleViewFields(SIMPLE_BANS)).isNotExpired(currentTime))
				// Pair with mutes
				.leftJoin(SIMPLE_MUTES)
				.on(new VictimCondition(new SimpleViewFields(SIMPLE_MUTES)).matchesUUID(detectedAlt.UUID))
				.and(new EndTimeCondition(new SimpleViewFields(SIMPLE_MUTES)).isNotExpired(currentTime))
				// Select alts for the player in question
				.where(ADDRESSES.UUID.eq(uuid))
				// Order with oldest first
				.orderBy(detectedAlt.UPDATED.asc())
				.fetch((record) -> {
					NetworkAddress detectedAddress = record.get(detectedAlt.ADDRESS);
					// If this alt can be detected 'normally', then the address will be the same
					DetectionKind detectionKind = (address.equals(detectedAddress)) ? DetectionKind.NORMAL : DetectionKind.STRICT;
					// Determine most significant punishment
					PunishmentType punishmentType;
					if (record.get(hasBan)) {
						punishmentType = PunishmentType.BAN;
					} else if (record.get(hasMute)) {
						punishmentType = PunishmentType.MUTE;
					} else {
						punishmentType = null;
					}
					return new DetectedAlt(
							detectionKind,
							punishmentType,
							detectedAddress,
							record.get(detectedAlt.UUID),
							record.get(LATEST_NAMES.NAME),
							record.get(detectedAlt.UPDATED)
					);
				});
		switch (whichAlts) {
		case ALL_ALTS:
			break;
		case BANNED_OR_MUTED_ALTS:
			detectedAlts.removeIf((alt) -> alt.punishmentType().isEmpty());
			break;
		case BANNED_ALTS:
			detectedAlts.removeIf((alt) -> alt.punishmentType().orElse(null) != PunishmentType.BAN);
			break;
		default:
			throw new IllegalArgumentException("Unknown WhichAlts " + whichAlts);
		}
		return detectedAlts;
	}

	public CentralisedFuture<List<DetectedAlt>> detectAlts(UUID uuid, NetworkAddress address, WhichAlts whichAlts) {
		return queryExecutor.get().query(SQLFunction.readOnly((context) -> {
			return detectAlts(context, uuid, address, whichAlts);
		}));
	}

	public CentralisedFuture<List<DetectedAlt>> detectAlts(UUIDAndAddress userDetails, WhichAlts whichAlts) {
		return detectAlts(userDetails.uuid(), userDetails.address(), whichAlts);
	}

}
