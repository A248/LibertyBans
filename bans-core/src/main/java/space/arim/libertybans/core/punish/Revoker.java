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

package space.arim.libertybans.core.punish;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.RevocationOrder;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.database.sql.EndTimeCondition;
import space.arim.libertybans.core.database.sql.TableForType;
import space.arim.libertybans.core.database.sql.VictimCondition;
import space.arim.libertybans.core.database.sql.VictimFields;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.time.Instant;
import java.util.List;

import static space.arim.libertybans.core.schema.tables.Punishments.PUNISHMENTS;
import static space.arim.libertybans.core.schema.tables.SimpleActive.SIMPLE_ACTIVE;
import static space.arim.libertybans.core.schema.tables.SimpleHistory.SIMPLE_HISTORY;

@Singleton
public class Revoker implements InternalRevoker {

	private final FactoryOfTheFuture futuresFactory;
	private final Provider<InternalDatabase> dbProvider;
	private final PunishmentCreator creator;
	private final GlobalEnforcement enforcement;
	private final Time time;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	@Inject
	public Revoker(FactoryOfTheFuture futuresFactory, Provider<InternalDatabase> dbProvider,
				   PunishmentCreator creator, GlobalEnforcement enforcement, Time time) {
		this.futuresFactory = futuresFactory;
		this.dbProvider = dbProvider;
		this.creator = creator;
		this.enforcement = enforcement;
		this.time = time;
	}

	FactoryOfTheFuture futuresFactory() {
		return futuresFactory;
	}

	GlobalEnforcement enforcement() {
		return enforcement;
	}

	@Override
	public CentralisedFuture<Boolean> undoPunishment(final Punishment punishment) {
		if (punishment.isExpired(time.toJdkClock())) {
			// Already expired
			return futuresFactory.completedFuture(false);
		}
		return undoPunishmentByIdAndType(punishment.getIdentifier(), punishment.getType());
	}

	@Override
	public RevocationOrder revokeByIdAndType(long id, PunishmentType type) {
		return new RevocationOrderImpl(this, id, type);
	}

	@Override
	public RevocationOrder revokeById(long id) {
		return new RevocationOrderImpl(this, id);
	}

	@Override
	public RevocationOrder revokeByTypeAndVictim(PunishmentType type, Victim victim) {
		return revokeByTypeAndPossibleVictims(type, List.of(victim));
	}

	@Override
	public RevocationOrder revokeByTypeAndPossibleVictims(PunishmentType type, List<Victim> victims) {
		return new RevocationOrderImpl(this, type, victims);
	}

	private boolean deleteActivePunishmentByIdAndType(DSLContext context,
													  final long id, final PunishmentType type) {
		final Instant currentTime = time.currentTimestamp();

		var dataTable = new TableForType(type).dataTable();
		int deleteCount = context
				.deleteFrom(dataTable.table())
				.where(dataTable.id().eq(id))
				.execute();
		logger.trace("deleteCount={} in deleteActivePunishmentByIdAndType", deleteCount);
		if (deleteCount != 1) {
			assert deleteCount == 0;
			return false;
		}
		boolean wasNotExpired = context.fetchExists(context
				.selectFrom(PUNISHMENTS)
				.where(PUNISHMENTS.ID.eq(id))
				.and(new EndTimeCondition(PUNISHMENTS.END).isNotExpired(currentTime))
		);
		logger.trace("wasNotExpired={} in deleteActivePunishmentByIdAndType", wasNotExpired);
		return wasNotExpired;
	}

	private Punishment deleteAndGetActivePunishmentByIdAndType(DSLContext context,
															   final long id, final PunishmentType type) {
		final Instant currentTime = time.currentTimestamp();

		var dataTable = new TableForType(type).dataTable();
		int deleteCount = context
				.deleteFrom(dataTable.table())
				.where(dataTable.id().eq(id))
				.execute();
		logger.trace("deleteCount={} in deleteAndGetActivePunishmentByIdAndType", deleteCount);
		if (deleteCount != 1) {
			return null;
		}
		Punishment result = context
				.select(
						SIMPLE_HISTORY.VICTIM_TYPE, SIMPLE_HISTORY.VICTIM_UUID, SIMPLE_HISTORY.VICTIM_ADDRESS,
						SIMPLE_HISTORY.OPERATOR, SIMPLE_HISTORY.REASON,
						SIMPLE_HISTORY.SCOPE, SIMPLE_HISTORY.START, SIMPLE_HISTORY.END
				)
				.from(SIMPLE_HISTORY)
				.where(SIMPLE_HISTORY.ID.eq(id))
				// If the punishment was expired, return null
				.and(new EndTimeCondition(SIMPLE_HISTORY.END).isNotExpired(currentTime))
				.fetchOne(creator.punishmentMapper(id, type));
		logger.trace("result={} in deleteAndGetActivePunishmentByIdAndType", result);
		return result;
	}

	CentralisedFuture<Boolean> undoPunishmentByIdAndType(final long id, final PunishmentType type) {
		if (type == PunishmentType.KICK) {
			// Kicks are never active
			return futuresFactory.completedFuture(false);
		}
		InternalDatabase database = dbProvider.get();
		return database.queryWithRetry((context, transaction) -> {
			return deleteActivePunishmentByIdAndType(context, id, type);
		});
	}

	CentralisedFuture<Punishment> undoAndGetPunishmentByIdAndType(final long id, final PunishmentType type) {
		if (type == PunishmentType.KICK) {
			// Kicks are never active
			return futuresFactory.completedFuture(null);
		}
		InternalDatabase database = dbProvider.get();
		return database.queryWithRetry((context, transaction) -> {
			return deleteAndGetActivePunishmentByIdAndType(context, id, type);
		});
	}

	CentralisedFuture<PunishmentType> undoPunishmentById(final long id) {
		InternalDatabase database = dbProvider.get();
		return database.queryWithRetry((context, transaction) -> {
			PunishmentType type = context
					.select(SIMPLE_ACTIVE.TYPE)
					.from(SIMPLE_ACTIVE)
					.where(SIMPLE_ACTIVE.ID.eq(id))
					.fetchSingle(SIMPLE_ACTIVE.TYPE);
			logger.trace("type={} in undoPunishmentById", type);
			if (type == null || !deleteActivePunishmentByIdAndType(context, id, type)) {
				return null;
			}
			return type;
		});
	}

	CentralisedFuture<Punishment> undoAndGetPunishmentById(final long id) {
		InternalDatabase database = dbProvider.get();
		return database.queryWithRetry((context, transaction) -> {
			PunishmentType type = context
					.select(SIMPLE_ACTIVE.TYPE)
					.from(SIMPLE_ACTIVE)
					.where(SIMPLE_ACTIVE.ID.eq(id))
					.fetchSingle(SIMPLE_ACTIVE.TYPE);
			logger.trace("type={} in undoAndGetPunishmentById", type);
			if (type == null) {
				return null;
			}
			return deleteAndGetActivePunishmentByIdAndType(context, id, type);
		});
	}

	private static Condition matchesAnyVictim(VictimFields victimFields, List<Victim> victims) {
		Condition matchesAnyVictim = DSL.noCondition();
		VictimCondition victimCondition = new VictimCondition(victimFields);
		for (Victim victim : victims) {
			matchesAnyVictim = matchesAnyVictim.or(
					victimCondition.matchesVictim(victim)
			);
		}
		return matchesAnyVictim;
	}

	CentralisedFuture<Long> undoPunishmentByTypeAndPossibleVictims(final PunishmentType type, 
																   final List<Victim> victims) {
		InternalDatabase database = dbProvider.get();
		return database.queryWithRetry((context, transaction) -> {
			var simpleView = new TableForType(type).simpleView();
			Long id = context
					.select(simpleView.id())
					.from(simpleView.table())
					.where(matchesAnyVictim(simpleView, victims))
					.fetchAny(simpleView.id());
			logger.trace("id={} in undoPunishmentByTypeAndVictim", id);
			if (id == null || !deleteActivePunishmentByIdAndType(context, id, type)) {
				return null;
			}
			return id;
		});
	}

	CentralisedFuture<Punishment> undoAndGetPunishmentByTypeAndPossibleVictims(final PunishmentType type, 
																			   final List<Victim> victims) {
		InternalDatabase database = dbProvider.get();
		return database.queryWithRetry((context, transaction) -> {
			var simpleView = new TableForType(type).simpleView();
			Long id = context
					.select(simpleView.id())
					.from(simpleView.table())
					.where(matchesAnyVictim(simpleView, victims))
					.fetchAny(simpleView.id());
			logger.trace("id={} in undoAndGetPunishmentByTypeAndVictim", id);
			if (id == null) {
				return null;
			}
			return deleteAndGetActivePunishmentByIdAndType(context, id, type);
		});
	}
	
}
