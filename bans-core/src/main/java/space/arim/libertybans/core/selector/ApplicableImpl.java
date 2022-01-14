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

package space.arim.libertybans.core.selector;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.sql.EndTimeCondition;
import space.arim.libertybans.core.database.sql.SimpleViewFields;
import space.arim.libertybans.core.database.sql.VictimCondition;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.core.punish.PunishmentCreator;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static space.arim.libertybans.core.schema.tables.ApplicableActive.APPLICABLE_ACTIVE;
import static space.arim.libertybans.core.schema.tables.SimpleActive.SIMPLE_ACTIVE;
import static space.arim.libertybans.core.schema.tables.StrictLinks.STRICT_LINKS;

@Singleton
public class ApplicableImpl {

	private final Configs configs;
	private final FactoryOfTheFuture futuresFactory;
	private final Provider<InternalDatabase> dbProvider;
	private final PunishmentCreator creator;

	private final Time time;

	@Inject
	public ApplicableImpl(Configs configs, FactoryOfTheFuture futuresFactory,
						  Provider<InternalDatabase> dbProvider, PunishmentCreator creator,
						  Time time) {
		this.configs = configs;
		this.futuresFactory = futuresFactory;
		this.dbProvider = dbProvider;
		this.creator = creator;
		this.time = time;
	}

	Punishment selectApplicable(DSLContext context,
								UUID uuid, NetworkAddress address,
								PunishmentType type, final Instant currentTime) {
		var appl = APPLICABLE_ACTIVE;
		AddressStrictness strictness = configs.getMainConfig().enforcement().addressStrictness();
		switch (strictness) {
		case LENIENT:
			return context
					.select(
							SIMPLE_ACTIVE.ID,
							SIMPLE_ACTIVE.VICTIM_TYPE, SIMPLE_ACTIVE.VICTIM_UUID, SIMPLE_ACTIVE.VICTIM_ADDRESS,
							SIMPLE_ACTIVE.OPERATOR, SIMPLE_ACTIVE.REASON,
							SIMPLE_ACTIVE.SCOPE, SIMPLE_ACTIVE.START, SIMPLE_ACTIVE.END
					)
					.from(SIMPLE_ACTIVE)
					.where(SIMPLE_ACTIVE.TYPE.eq(type))
					.and(new VictimCondition(new SimpleViewFields(SIMPLE_ACTIVE)).simplyMatches(DSL.val(uuid), DSL.val(address)))
					.and(new EndTimeCondition(SIMPLE_ACTIVE.END).isNotExpired(currentTime))
					.limit(1)
					.fetchOne(creator.punishmentMapper(type));
		case NORMAL:
			return context
					.select(
							appl.ID,
							appl.VICTIM_TYPE, appl.VICTIM_UUID, appl.VICTIM_ADDRESS,
							appl.OPERATOR, appl.REASON,
							appl.SCOPE, appl.START, appl.END
					).from(appl)
					.where(appl.TYPE.eq(type))
					.and(appl.UUID.eq(uuid))
					.and(new EndTimeCondition(appl.END).isNotExpired(currentTime))
					.limit(1)
					.fetchOne(creator.punishmentMapper(type));
		case STRICT:
			return context
					.select(
							appl.ID,
							appl.VICTIM_TYPE, appl.VICTIM_UUID, appl.VICTIM_ADDRESS,
							appl.OPERATOR, appl.REASON,
							appl.SCOPE, appl.START, appl.END
					).from(appl)
					.innerJoin(STRICT_LINKS)
					.on(appl.UUID.eq(STRICT_LINKS.UUID1))
					.where(appl.TYPE.eq(type))
					.and(STRICT_LINKS.UUID2.eq(uuid))
					.and(new EndTimeCondition(appl.END).isNotExpired(currentTime))
					.limit(1)
					.fetchOne(creator.punishmentMapper(type));
		default:
			throw MiscUtil.unknownAddressStrictness(strictness);
		}
	}

	private CentralisedFuture<Punishment> getApplicablePunishment0(UUID uuid, NetworkAddress address, PunishmentType type) {
		InternalDatabase database = dbProvider.get();
		return database.query(SQLFunction.readOnly((context) -> {
			return selectApplicable(context, uuid, address, type, time.currentTimestamp());
		}));
	}

	CentralisedFuture<Punishment> getApplicablePunishment(UUID uuid, NetworkAddress address, PunishmentType type) {
		Objects.requireNonNull(type, "type");
		if (type == PunishmentType.KICK) {
			// Kicks are never active
			return futuresFactory.completedFuture(null);
		}
		return getApplicablePunishment0(uuid, address, type);
	}
	
	CentralisedFuture<Punishment> getApplicableMute(UUID uuid, NetworkAddress address) {
		return getApplicablePunishment0(uuid, address, PunishmentType.MUTE);
	}

}
