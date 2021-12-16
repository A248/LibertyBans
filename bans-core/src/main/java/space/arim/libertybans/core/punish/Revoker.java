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
package space.arim.libertybans.core.punish;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.revoke.RevocationOrder;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.database.sql.EndTimeCondition;
import space.arim.libertybans.core.database.sql.SimpleViewFields;
import space.arim.libertybans.core.database.sql.TableForType;
import space.arim.libertybans.core.database.sql.VictimCondition;
import space.arim.libertybans.core.selector.MuteCache;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.time.Instant;

import static space.arim.libertybans.core.schema.tables.Punishments.PUNISHMENTS;
import static space.arim.libertybans.core.schema.tables.SimpleActive.SIMPLE_ACTIVE;
import static space.arim.libertybans.core.schema.tables.SimpleHistory.SIMPLE_HISTORY;

@Singleton
public class Revoker implements InternalRevoker {

	private final FactoryOfTheFuture futuresFactory;
	private final Provider<InternalDatabase> dbProvider;
	private final PunishmentCreator creator;
	private final MuteCache muteCache;
	private final Time time;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	@Inject
	public Revoker(FactoryOfTheFuture futuresFactory, Provider<InternalDatabase> dbProvider,
				   PunishmentCreator creator, MuteCache muteCache, Time time) {
		this.futuresFactory = futuresFactory;
		this.dbProvider = dbProvider;
		this.creator = creator;
		this.muteCache = muteCache;
		this.time = time;
	}
	
	MuteCache muteCache() {
		return muteCache;
	}
	
	@Override
	public CentralisedFuture<Boolean> undoPunishment(final Punishment punishment) {
		if (punishment.isExpired()) {
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
		return new RevocationOrderImpl(this, type, victim);
	}

	private boolean deleteActivePunishmentByIdAndType(DSLContext context,
													  final long id, final PunishmentType type) {
		final Instant currentTime = time.currentTimestamp();

		var table = new TableForType(type).dataTable();
		var fields = table.newRecord();

		int deleteCount = context
				.deleteFrom(table)
				.where(fields.field1().eq(id))
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

		var table = new TableForType(type).dataTable();
		var fields = table.newRecord();

		int deleteCount = context
				.deleteFrom(table)
				.where(fields.field1().eq(id))
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
	
	CentralisedFuture<Boolean> undoPunishmentById(final long id) {
		InternalDatabase database = dbProvider.get();
		return database.queryWithRetry((context, transaction) -> {
			PunishmentType type = context
					.select(SIMPLE_ACTIVE.TYPE)
					.from(SIMPLE_ACTIVE)
					.where(SIMPLE_ACTIVE.ID.eq(id))
					.fetchSingle(SIMPLE_ACTIVE.TYPE);
			logger.trace("type={} in undoPunishmentById", type);
			return type != null
					&& deleteActivePunishmentByIdAndType(context, id, type);
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

	CentralisedFuture<Boolean> undoPunishmentByTypeAndVictim(final PunishmentType type, final Victim victim) {
		MiscUtil.checkSingular(type);
		InternalDatabase database = dbProvider.get();
		return database.queryWithRetry((context, transaction) -> {
			Long id = context
					.select(SIMPLE_ACTIVE.ID)
					.from(SIMPLE_ACTIVE)
					.where(new VictimCondition(new SimpleViewFields(SIMPLE_ACTIVE)).matchesVictim(victim))
					.fetchOne(SIMPLE_ACTIVE.ID);
			logger.trace("id={} in undoPunishmentByTypeAndVictim", id);
			return id != null && deleteActivePunishmentByIdAndType(context, id, type);
		});
	}

	CentralisedFuture<Punishment> undoAndGetPunishmentByTypeAndVictim(final PunishmentType type, final Victim victim) {
		InternalDatabase database = dbProvider.get();
		return database.queryWithRetry((context, transaction) -> {
			Long id = context
					.select(SIMPLE_ACTIVE.ID)
					.from(SIMPLE_ACTIVE)
					.where(new VictimCondition(new SimpleViewFields(SIMPLE_ACTIVE)).matchesVictim(victim))
					.fetchOne(SIMPLE_ACTIVE.ID);
			logger.trace("id={} in undoAndGetPunishmentByTypeAndVictim", id);
			if (id == null) {
				return null;
			}
			return deleteAndGetActivePunishmentByIdAndType(context, id, type);
		});
	}
	
}
