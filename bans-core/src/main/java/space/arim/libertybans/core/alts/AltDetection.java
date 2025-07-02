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
import jakarta.inject.Provider;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SelectField;
import org.jooq.Table;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.user.AltAccount;
import space.arim.libertybans.api.user.AltDetectionQuery;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.database.pagination.*;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.sql.AccountExpirationCondition;
import space.arim.libertybans.core.database.sql.EndTimeCondition;
import space.arim.libertybans.core.database.sql.TableForType;
import space.arim.libertybans.core.database.sql.VictimCondition;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.jooq.impl.DSL.*;
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
	 * Detects alts for the given account.
	 *
	 * @param context the query source with which to contact the database
	 * @param query the detection query
	 * @return the response
	 */
	private KeysetPage<DetectedAlt, InstantThenUUID> detectAlts(DSLContext context, AltQuery query) {
 		/*

 		Alt detection

 		This implementation includes multiple components. We need to detect non-expired alts, fetch their names,
 		and order them by time, UUID pair. We also need to fetch punishment information (bans, mutes, warns) and in
 		some cases, remove alts which don't have any punishments directly on them.

 		Pagination is handled for us. However, to make the keyset/seek method work, we can't perform post-processing.
 		Thus filtering for "has_ban" and "has_mute" must happen in the join, and we decide between LEFT or INNER join
 		to make that distinction.

 		The implementation fetches both strictly detected and normally detected alts. We know if an alt was only
 		strictly detected if the alt address is different from the input address.
 		 */
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
				// Filter non-expired alts
				.and(new AccountExpirationCondition(detectedAlt.UPDATED).isNotExpired(configs, currentTime))
				// Pair with latest names
				.leftJoin(LATEST_NAMES)
				.on(LATEST_NAMES.UUID.eq(detectedAlt.UUID));

		Map<PunishmentType, Field<Victim.VictimType>> typeMatches = new EnumMap<>(PunishmentType.class);
		for (PunishmentType type : query.punishmentTypes()) {
			// Pair with that particular type
			var simpleView = new TableForType(type).simpleView();
			joinedTables = joinedTables
					.leftJoin(simpleView.table())
					.on(new VictimCondition(simpleView).matchesUUID(detectedAlt.UUID))
					.and(new EndTimeCondition(simpleView).isNotExpired(currentTime));
			var victimTypeField = simpleView.victimType();
			typeMatches.put(type, victimTypeField);
			selectFields.add(victimTypeField);
		}
		AltInfoRequest request = query.request;
		Pagination<InstantThenUUID> pagination = new Pagination<>(
				request.pageAnchor(), request.oldestFirst(),
				InstantThenUUID.defineOrder(detectedAlt.UPDATED, detectedAlt.UUID)
		);
		List<DetectedAlt> detectedAlts = context
				.select(selectFields)
				.from(joinedTables)
				// Select alts for the player in question
				.where(ADDRESSES.UUID.eq(query.uuid()))
				// Filter based on punishments matched
				.and(switch (request.filter()) {
                    case ALL_ALTS -> noCondition();
                    case BANNED_OR_MUTED_ALTS -> typeMatches.get(PunishmentType.BAN).isNotNull()
							.or(typeMatches.get(PunishmentType.MUTE).isNotNull());
                    case BANNED_ALTS -> typeMatches.get(PunishmentType.BAN).isNotNull();
                })
				// Sorting and pagination
				.and(pagination.seeking())
				.orderBy(pagination.order())
				.limit(request.pageSize())
				.offset(request.skipCount())
				.fetch((record) -> {
					NetworkAddress detectedAddress = record.get(detectedAlt.ADDRESS);
					// If this alt can be detected 'normally', then the address will be the same
					DetectionKind detectionKind = (query.address().equals(detectedAddress)) ? DetectionKind.NORMAL : DetectionKind.STRICT;
					// Determine scanned punishment types
					Set<PunishmentType> scannedTypes = EnumSet.noneOf(PunishmentType.class);
					for (PunishmentType scanFor : query.punishmentTypes()) {
						var hasType = typeMatches.get(scanFor).get(record) != null;
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
		return pagination.anchor().buildPage(detectedAlts, new KeysetPage.AnchorLiaison<>() {

            @Override
            public BorderValueHandle<InstantThenUUID> borderValueHandle() {
                return InstantThenUUID.borderValueHandle();
            }

            @Override
            public InstantThenUUID getAnchor(DetectedAlt datum) {
                return new InstantThenUUID(datum.recorded(), datum.uuid());
            }
        });
	}

	record AltQuery(AltInfoRequest request,
					Set<PunishmentType> punishmentTypes, AltDetection impl) implements AltDetectionQuery {

		@Override
		public UUID uuid() {
			return request.uuid();
		}

		@Override
		public NetworkAddress address() {
			return request.address();
		}

		@Override
		public CentralisedFuture<List<? extends AltAccount>> detect() {
			return impl.queryExecutor.get().query(SQLFunction.readOnly(
					(context) -> impl.detectAlts(context, request)
			)).thenApply(KeysetPage::data);
		}

	}

	public KeysetPage<DetectedAlt, InstantThenUUID> detectAlts(DSLContext context, AltInfoRequest retrieval) {
		return detectAlts(
				context,
				new AltQuery(retrieval, Set.of(PunishmentType.BAN, PunishmentType.MUTE), this)
		);
	}

	public CentralisedFuture<KeysetPage<DetectedAlt, InstantThenUUID>> detectAlts(AltInfoRequest retrieval) {
		return queryExecutor.get().query(SQLFunction.readOnly((context) -> detectAlts(context, retrieval)));
	}

}
