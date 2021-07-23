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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.revoke.RevocationOrder;
import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.selector.MuteCache;

import space.arim.jdbcaesar.query.ResultSetConcurrency;

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
		return undoPunishmentByIdAndType(punishment.getID(), punishment.getType());
	}
	
	@Override
	public RevocationOrder revokeByIdAndType(int id, PunishmentType type) {
		return new RevocationOrderImpl(this, id, type);
	}

	@Override
	public RevocationOrder revokeById(int id) {
		return new RevocationOrderImpl(this, id);
	}

	@Override
	public RevocationOrder revokeByTypeAndVictim(PunishmentType type, Victim victim) {
		return new RevocationOrderImpl(this, type, victim);
	}
	
	CentralisedFuture<Boolean> undoPunishmentByIdAndType(final int id, final PunishmentType type) {
		if (type == PunishmentType.KICK) {
			// Kicks are never active
			return futuresFactory.completedFuture(false);
		}
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			final long currentTime = time.currentTime();

			if (database.getVendor().hasDeleteFromJoin()) {
				return database.jdbCaesar().query(
						"DELETE `thetype` FROM `libertybans_" + type + "s` `thetype` "
								+ "INNER JOIN `libertybans_punishments` `puns` ON `thetype`.`id` = `puns`.`id` "
								+ "WHERE `thetype`.`id` = ? AND (`puns`.`end` = 0 OR `puns`.`end` > ?)")
						.params(id, currentTime)
						.updateCount((updateCount) -> updateCount == 1)
						.execute();
			}
			return database.jdbCaesar().transaction().body((querySource, controller) -> {

				boolean deleted = querySource.query(
						"DELETE FROM `libertybans_" + type + "s` WHERE `id` = ?")
						.params(id)
						.updateCount((updateCount) -> updateCount == 1)
						.execute();
				logger.trace("deleted={} in undoPunishmentByIdAndType", deleted);
				if (!deleted) {
					return false;
				}
				long end = querySource.query(
						"SELECT `end` FROM `libertybans_punishments` WHERE `id` = ?")
						.params(id)
						.singleResult(database::getEndFromResult).execute();
				boolean expired = MiscUtil.isExpired(currentTime, end);
				logger.trace("expired={} in undoPunishmentByIdAndType", expired);
				return !expired;

			}).execute();
		});
	}
	
	CentralisedFuture<Punishment> undoAndGetPunishmentByIdAndType(final int id, final PunishmentType type) {
		if (type == PunishmentType.KICK) {
			// Kicks are never active
			return futuresFactory.completedFuture(null);
		}
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			final long currentTime = time.currentTime();
			return database.jdbCaesar().transaction().body((querySource, controller) -> {

				Victim victim = querySource.query(
						// MariaDB-Connector requires the primary key to be present for ResultSet#deleteRow
						"SELECT `id`, `victim`, `victim_type` FROM `libertybans_" + type + "s` "
						+ "WHERE `id` = ? FOR UPDATE")
						.params(id)
						.resultSetConcurrency(ResultSetConcurrency.UPDATABLE)

						.singleResult((resultSet) -> {
							Victim found = database.getVictimFromResult(resultSet);
							// Either the punishment is active and will be undone
							// Or it is expired and may be cleaned out
							resultSet.deleteRow();
							return found;
						}).execute();

				logger.trace("victim={} in undoAndGetPunishmentByIdAndType", victim);
				if (victim == null) {
					return null;
				}
				Punishment result = querySource.query(
						"SELECT `operator`, `reason`, `scope`, `start`, `end` FROM `libertybans_punishments` "
						+ "WHERE `id` = ? AND (`end` = 0 OR `end` > ?)")
						.params(id, currentTime)
						.singleResult((resultSet) -> {
							return creator.createPunishment(id, type, victim, database.getOperatorFromResult(resultSet),
									database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
									database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
						}).execute();
				logger.trace("result={} in undoAndGetPunishmentByIdAndType", result);
				return result;
			}).execute();
		});
	}
	
	CentralisedFuture<Boolean> undoPunishmentById(final int id) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			final long currentTime = time.currentTime();

			return database.jdbCaesar().transaction().body((querySource, controller) -> {
				boolean hasDeleteFromJoin = database.getVendor().hasDeleteFromJoin();

				for (PunishmentType type : MiscUtil.punishmentTypesExcludingKick()) {

					if (hasDeleteFromJoin) {
						boolean deleted = querySource.query(
								"DELETE `thetype` FROM `libertybans_" + type + "s` `thetype` "
										+ "INNER JOIN `libertybans_punishments` `puns` ON `thetype`.`id` = `puns`.`id` "
										+ "WHERE `thetype`.`id` = ? AND (`end` = 0 OR `end` > ?)")
								.params(id, currentTime)
								.updateCount((updateCount) -> updateCount == 1)
								.execute();
						if (deleted) {
							return true;
						}
					} else {
						boolean deleted = querySource.query(
								"DELETE FROM `libertybans_" + type + "s` WHERE `id` = ?")
								.params(id)
								.updateCount((updateCount) -> updateCount == 1)
								.execute();
						logger.trace("deleted={} in undoPunishmentById", deleted);
						if (deleted) {
							long end = querySource.query(
									"SELECT `end` FROM `libertybans_punishments` WHERE `id` = ?")
									.params(id)
									.singleResult(database::getEndFromResult).execute();
							boolean expired = MiscUtil.isExpired(currentTime, end);
							logger.trace("expired={} in undoPunishmentById", expired);
							return !expired;
						}
					}
				}
				return false;
			}).execute();
		});
	}
	
	CentralisedFuture<Punishment> undoAndGetPunishmentById(final int id) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			return database.jdbCaesar().transaction().body((querySource, controller) -> {

				final long currentTime = time.currentTime();
				for (PunishmentType type : MiscUtil.punishmentTypesExcludingKick()) {

					Victim victim = querySource.query(
							// MariaDB-Connector requires the primary key to be present for ResultSet#deleteRow
							"SELECT `id`, `victim`, `victim_type` FROM `libertybans_" + type + "s` "
							+ "WHERE `id` = ? FOR UPDATE")
							.params(id)
							.resultSetConcurrency(ResultSetConcurrency.UPDATABLE)

							.singleResult((resultSet) -> {
								Victim found = database.getVictimFromResult(resultSet);
								// Either the punishment is active and will be undone
								// Or it is expired and may be cleaned out
								resultSet.deleteRow();
								return found;
							}).execute();

					logger.trace("victim={} in undoAndGetPunishmentById", victim);
					if (victim != null) {
						Punishment result = querySource.query(
								"SELECT `operator`, `reason`, `scope`, `start`, `end` FROM `libertybans_punishments` "
								+ "WHERE `id` = ? AND (`end` = 0 OR `end` > ?)")
								.params(id, currentTime)
								.singleResult((resultSet) -> {
									return creator.createPunishment(id, type, victim, database.getOperatorFromResult(resultSet),
											database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
											database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
								}).execute();
						logger.trace("result={} in undoAndGetPunishmentById", result);
						return result;
					}
				}
				return null;
			}).execute();
		});
	}
	
	CentralisedFuture<Boolean> undoPunishmentByTypeAndVictim(final PunishmentType type, final Victim victim) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			final long currentTime = time.currentTime();

			if (database.getVendor().hasDeleteFromJoin()) {
				return database.jdbCaesar().query(
						"DELETE `thetype` FROM `libertybans_" + type + "s` `thetype` "
								+ "INNER JOIN `libertybans_punishments` `puns` ON `thetype`.`id` = `puns`.`id` "
								+ "WHERE `thetype`.`victim` = ? AND `thetype`.`victim_type` = ? "
								+ "AND (`end` = 0 OR `end` > ?)")
						.params(victim, victim.getType(), currentTime)
						.updateCount((updateCount) -> updateCount == 1)
						.execute();
			}
			return database.jdbCaesar().transaction().body((querySource, controller) -> {

				Integer id = querySource.query(
						"SELECT `id` FROM `libertybans_" + type + "s` "
						+ "WHERE `victim` = ? AND `victim_type` = ? FOR UPDATE")
						.params(victim, victim.getType())
						.resultSetConcurrency(ResultSetConcurrency.UPDATABLE)

						.singleResult((resultSet) -> {
							// Either the punishment is active and will be undone
							// Or it is expired and may be cleaned out
							int foundId = resultSet.getInt("id");
							resultSet.deleteRow();
							return foundId;
						}).execute();
				logger.trace("id={} in undoPunishmentByTypeAndVictim", id);
				if (id == null) {
					return false;
				}
				long end = querySource.query(
						"SELECT `end` FROM `libertybans_punishments` WHERE `id` = ?")
						.params(id)
						.singleResult(database::getEndFromResult).execute();
				boolean expired = MiscUtil.isExpired(currentTime, end);
				logger.trace("expired={} in undoPunishmentByTypeAndVictim", expired);
				return !expired;
			}).execute();
		});
	}

	CentralisedFuture<Punishment> undoAndGetPunishmentByTypeAndVictim(final PunishmentType type, final Victim victim) {
		InternalDatabase database = dbProvider.get();
		return database.selectAsync(() -> {
			final long currentTime = time.currentTime();
			return database.jdbCaesar().transaction().body((querySource, controller) -> {

				Integer id = querySource.query(
						"SELECT `id` FROM `libertybans_" + type + "s` "
						+ "WHERE `victim` = ? AND `victim_type` = ? FOR UPDATE")
						.params(victim, victim.getType())
						.resultSetConcurrency(ResultSetConcurrency.UPDATABLE)

						.singleResult((resultSet) -> {
							// Either the punishment is active and will be undone
							// Or it is expired and may be cleaned out
							int foundId = resultSet.getInt("id");
							resultSet.deleteRow();
							return foundId;
						}).execute();

				logger.trace("id={} in undoAndGetPunishmentByTypeAndVictim", id);
				if (id == null) {
					return null;
				}
				Punishment result = querySource.query(
						"SELECT `operator`, `reason`, `scope`, `start`, `end` FROM `libertybans_punishments` "
						+ "WHERE `id` = ? AND (`end` = 0 OR `end` > ?)")
						.params(id, currentTime)
						.singleResult((resultSet) -> {
							return creator.createPunishment(id, type,
									victim, database.getOperatorFromResult(resultSet),
									database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
									database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
						}).execute();
				logger.trace("result={} in undoAndGetPunishmentByTypeAndVictim", result);
				return result;
			}).execute();
		});
	}
	
}
