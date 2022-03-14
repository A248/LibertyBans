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
import space.arim.libertybans.core.database.sql.EndTimeOrdering;
import space.arim.libertybans.core.database.sql.TableForType;
import space.arim.libertybans.core.database.sql.VictimCondition;
import space.arim.libertybans.core.punish.MiscUtil;
import space.arim.libertybans.core.punish.PunishmentCreator;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

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
		var applView = new TableForType(type).applicableView();

		AddressStrictness strictness = configs.getMainConfig().enforcement().addressStrictness();
		switch (strictness) {
		case LENIENT:
			var simpleView = new TableForType(type).simpleView();
			return context
					.select(
							simpleView.id(),
							simpleView.victimType(), simpleView.victimUuid(), simpleView.victimAddress(),
							simpleView.operator(), simpleView.reason(),
							simpleView.scope(), simpleView.start(), simpleView.end()
					)
					.from(simpleView.table())
					.where(new VictimCondition(simpleView).simplyMatches(DSL.val(uuid), DSL.val(address)))
					.and(new EndTimeCondition(simpleView).isNotExpired(currentTime))
					.orderBy(new EndTimeOrdering(simpleView).expiresLeastSoon())
					.limit(1)
					.fetchOne(creator.punishmentMapper(type));
		case NORMAL:
			return context
					.select(
							applView.id(),
							applView.victimType(), applView.victimUuid(), applView.victimAddress(),
							applView.operator(), applView.reason(),
							applView.scope(), applView.start(), applView.end()
					).from(applView.table())
					.where(applView.uuid().eq(uuid))
					.and(new EndTimeCondition(applView).isNotExpired(currentTime))
					.orderBy(new EndTimeOrdering(applView).expiresLeastSoon())
					.limit(1)
					.fetchOne(creator.punishmentMapper(type));
		case STRICT:
			return context
					.select(
							applView.id(),
							applView.victimType(), applView.victimUuid(), applView.victimAddress(),
							applView.operator(), applView.reason(),
							applView.scope(), applView.start(), applView.end()
					).from(applView.table())
					.innerJoin(STRICT_LINKS)
					.on(applView.uuid().eq(STRICT_LINKS.UUID1))
					.where(STRICT_LINKS.UUID2.eq(uuid))
					.and(new EndTimeCondition(applView).isNotExpired(currentTime))
					.orderBy(new EndTimeOrdering(applView).expiresLeastSoon())
					.limit(1)
					.fetchOne(creator.punishmentMapper(type));
		default:
			throw MiscUtil.unknownAddressStrictness(strictness);
		}
	}

	CentralisedFuture<Punishment> getApplicablePunishment(UUID uuid, NetworkAddress address, PunishmentType type) {
		Objects.requireNonNull(type, "type");
		if (type == PunishmentType.KICK) {
			// Kicks are never active
			return futuresFactory.completedFuture(null);
		}
		return dbProvider.get().query(SQLFunction.readOnly((context) -> {
			return selectApplicable(context, uuid, address, type, time.currentTimestamp());
		}));
	}

}
