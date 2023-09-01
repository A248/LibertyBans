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
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SelectField;
import org.jooq.Table;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.user.AltAccount;
import space.arim.libertybans.api.user.AltDetectionQuery;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.sql.AccountExpirationCondition;
import space.arim.libertybans.core.database.sql.EndTimeCondition;
import space.arim.libertybans.core.database.sql.TableForType;
import space.arim.libertybans.core.database.sql.VictimCondition;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.jooq.impl.DSL.field;
import static space.arim.libertybans.core.schema.tables.Addresses.ADDRESSES;
import static space.arim.libertybans.core.schema.tables.LatestNames.LATEST_NAMES;

public class AltDetection {

	private final Configs configs;
	private final Provider<QueryExecutor> queryExecutor;
	private final Time time;

	@Inject
	public AltDetection(Configs configs, Provider<QueryExecutor> queryExecutor, Time time) {
		this.configs = configs;
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
	 * @param query the detection query
	 * @param whichAlts which alts to remove from the resulting list
	 * @return the detected alts, sorted in order of oldest first
	 */
	private List<DetectedAlt> detectAlts(DSLContext context, AltDetectionQuery query, WhichAlts whichAlts) {
		// This implementation relies on strict detection including normal detection
		// The detection kind is inferred while processing the results
		final Instant currentTime = time.currentTimestamp();
		var detectedAlt = ADDRESSES.as("detected_alt");

		List<SelectField<?>> selectFields = new ArrayList<>(List.of(
				detectedAlt.ADDRESS, detectedAlt.UUID,
				LATEST_NAMES.NAME, detectedAlt.UPDATED
		));
		Table<?> joinedTables = ADDRESSES
				// Detect alts
				.innerJoin(detectedAlt)
				.on(ADDRESSES.ADDRESS.eq(detectedAlt.ADDRESS))
				.and(ADDRESSES.UUID.notEqual(detectedAlt.UUID))
				// Pair with latest names
				.leftJoin(LATEST_NAMES)
				.on(LATEST_NAMES.UUID.eq(detectedAlt.UUID));

		Map<PunishmentType, Field<Boolean>> hasTypeFields = new EnumMap<>(PunishmentType.class);
		for (PunishmentType type : query.punishmentTypes()) {
			// Pair with that particular type
			var simpleView = new TableForType(type).simpleView();
			joinedTables = joinedTables
					.leftJoin(simpleView.table())
					.on(new VictimCondition(simpleView).matchesUUID(detectedAlt.UUID))
					.and(new EndTimeCondition(simpleView).isNotExpired(currentTime));
			Field<Boolean> hasTypeField = field(simpleView.victimType().isNotNull().as("has_" + type.toString().toLowerCase(Locale.ROOT)));
			hasTypeFields.put(type, hasTypeField);
			selectFields.add(hasTypeField);
		}
		List<DetectedAlt> detectedAlts = context
				.select(selectFields)
				.from(joinedTables)
				// Select alts for the player in question
				.where(ADDRESSES.UUID.eq(query.uuid()))
				// Filter non-expired alts
				.and(new AccountExpirationCondition(detectedAlt.UPDATED).isNotExpired(configs, currentTime))
				// Order with oldest first
				.orderBy(detectedAlt.UPDATED.asc())
				.fetch((record) -> {
					NetworkAddress detectedAddress = record.get(detectedAlt.ADDRESS);
					// If this alt can be detected 'normally', then the address will be the same
					DetectionKind detectionKind = (query.address().equals(detectedAddress)) ? DetectionKind.NORMAL : DetectionKind.STRICT;
					// Determine scanned scannedTypes
					Set<PunishmentType> scannedTypes = EnumSet.noneOf(PunishmentType.class);
					for (PunishmentType scanFor : query.punishmentTypes()) {
						var hasType = hasTypeFields.get(scanFor).get(record);
						Objects.requireNonNull(hasType);
						if (hasType) {
							scannedTypes.add(scanFor);
						}
					}
					return new DetectedAlt(
							record.get(detectedAlt.UUID),
							record.get(LATEST_NAMES.NAME),
							detectedAddress,
							record.get(detectedAlt.UPDATED),
							detectionKind,
							scannedTypes
					);
				});
		Predicate<DetectedAlt> removeIf = switch (whichAlts) {
			case ALL_ALTS -> (alt) -> false;
			case BANNED_OR_MUTED_ALTS -> (alt) -> alt.scannedTypes().isEmpty();
			case BANNED_ALTS -> (alt) -> !alt.scannedTypes().contains(PunishmentType.BAN);
		};
		detectedAlts.removeIf(removeIf);
		return detectedAlts;
	}

	record AltQuery(UUID uuid, NetworkAddress address,
					Set<PunishmentType> punishmentTypes, AltDetection impl) implements AltDetectionQuery {

		@Override
		public CentralisedFuture<List<? extends AltAccount>> detect() {
			return impl.queryExecutor.get().query(SQLFunction.readOnly((context) -> {
				return impl.detectAlts(context, this, WhichAlts.ALL_ALTS);
			}));
		}

	}

	public List<DetectedAlt> detectAlts(DSLContext context, UUID uuid, NetworkAddress address, WhichAlts whichAlts) {
		return detectAlts(
				context,
				new AltQuery(uuid, address, Set.of(PunishmentType.BAN, PunishmentType.MUTE), this),
				whichAlts
		);
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
