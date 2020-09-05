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
package space.arim.libertybans.core;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.DraftPunishment;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentEnactor;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Scope;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.database.Database;

import space.arim.jdbcaesar.mapper.UpdateCountMapper;
import space.arim.jdbcaesar.query.ResultSetConcurrency;
import space.arim.jdbcaesar.transact.RollMeBackException;

public class Enactor implements PunishmentEnactor {
	
	private final LibertyBansCore core;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	Enactor(LibertyBansCore core) {
		this.core = core;
	}

	@Override
	public CentralisedFuture<Punishment> enactPunishment(DraftPunishment draftPunishment) {
		MiscUtil.validate(draftPunishment);
		Database database = core.getDatabase();
		return database.selectAsync(() -> {

			PunishmentType type = draftPunishment.getType();
			Victim victim = draftPunishment.getVictim();
			Operator operator = draftPunishment.getOperator();
			String reason = draftPunishment.getReason();
			Scope scope = draftPunishment.getScope();

			if (database.getVendor().useStoredRoutines()) {
				String enactmentProcedure = MiscUtil.getEnactmentProcedure(type);

				return database.jdbCaesar().query(
						"{CALL `libertybans_" + enactmentProcedure + "` (?, ?, ?, ?, ?, ?, ?)}")
						.params(victim, victim.getType().ordinal() + 1, operator, reason,
								scope, draftPunishment.getStart(), draftPunishment.getEnd())

						.singleResult((resultSet) -> {
							int id = resultSet.getInt("id");
							return new SecurePunishment(id, type, victim, operator, reason,
									scope, draftPunishment.getStart(), draftPunishment.getEnd());
						}).onError(() -> null).execute();
			} else {
				return database.jdbCaesar().transaction().transactor((querySource) -> {

					int id = querySource.query(
							"INSERT INTO `libertybans_punishments` (`type`, `operator`, `reason`, `scope`, `start`, `end`) "
							+ "VALUES (?, ?, ?, ?, ?, ?)")
							.params(type, operator, draftPunishment.getReason(), scope,
									draftPunishment.getStart(), draftPunishment.getEnd())
							.updateGenKeys((updateCount, genKeys) ->  {
								if (!genKeys.next()) {
									throw new IllegalStateException("No punishment ID generated for insertion query");
								}
								return genKeys.getInt("id");
							}).execute();

					String enactStatement = "%INSERT% INTO `libertybans_" + type.getLowercaseNamePlural() + "` "
							+ "(`id`, `victim`, `victim_type`) VALUES (?, ?, ?)";
					Object[] enactArgs = new Object[] {id, victim, victim.getType()};

					if (type.isSingular()) {
						enactStatement = enactStatement.replace("%INSERT%", "INSERT IGNORE");
						int updateCount = querySource.query(enactStatement).params(enactArgs)
								.updateCount(UpdateCountMapper.identity()).execute();
						if (updateCount == 0) {
							throw new RollMeBackException();
						}
					} else if (type != PunishmentType.KICK) { 
						enactStatement = enactStatement.replace("%INSERT%", "INSERT");
						querySource.query(enactStatement).params(enactArgs).voidResult().execute();
					}

					querySource.query(
							"INSERT INTO `libertybans_history` (`id`, `victim`, `victim_type`) VALUES (?, ?, ?)")
							.params(id, victim, victim.getType())
							.voidResult().execute();
					return new SecurePunishment(id, type, victim, operator, reason,
							scope, draftPunishment.getStart(), draftPunishment.getEnd());
				}).onRollback(() -> null).execute();
			}
		});
	}
	
	@Override
	public CentralisedFuture<Boolean> undoPunishment(final Punishment punishment) {
		MiscUtil.validate(punishment);
		PunishmentType type = punishment.getType();
		if (type == PunishmentType.KICK) {
			// Kicks are never active
			return core.getFuturesFactory().completedFuture(false);
		}
		final long currentTime = MiscUtil.currentTime();
		if (MiscUtil.isExpired(currentTime, punishment.getEnd())) {
			// Already expired
			return core.getFuturesFactory().completedFuture(false);
		}
		Database database = core.getDatabase();
		return database.selectAsync(() -> {

			return database.jdbCaesar().query(
					"DELETE FROM `libertybans_" + type.getLowercaseNamePlural() + "` WHERE `id` = ?")
					.params(punishment.getID())
					.updateCount((updateCount) -> updateCount == 1)
					.onError(() -> false).execute();
		});
	}
	
	@Override
	public CentralisedFuture<Boolean> undoPunishmentByIdAndType(final int id, final PunishmentType type) {
		Objects.requireNonNull(type, "type");
		if (type == PunishmentType.KICK) {
			// Kicks are never active
			return core.getFuturesFactory().completedFuture(false);
		}
		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			final long currentTime = MiscUtil.currentTime();
			return database.jdbCaesar().transaction().transactor((querySource) -> {

				boolean deleted = querySource.query(
						"DELETE FROM `libertybans_" + type.getLowercaseNamePlural() + "` WHERE `id` = ?")
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

			}).onRollback(() -> false).execute();
		});
	}
	
	@Override
	public CentralisedFuture<Punishment> undoAndGetPunishmentByIdAndType(final int id, final PunishmentType type) {
		Objects.requireNonNull(type, "type");
		if (type == PunishmentType.KICK) {
			// Kicks are never active
			return core.getFuturesFactory().completedFuture(null);
		}
		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			final long currentTime = MiscUtil.currentTime();
			return database.jdbCaesar().transaction().transactor((querySource) -> {

				Victim victim = querySource.query(
						// MariaDB-Connector requires the primary key to be present for ResultSet#deleteRow
						"SELECT `id`, `victim`, `victim_type` FROM `libertybans_" + type.getLowercaseNamePlural() + "` "
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
							return new SecurePunishment(id, type, victim, database.getOperatorFromResult(resultSet),
									database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
									database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
						}).execute();
				logger.trace("result={} in undoAndGetPunishmentByIdAndType", result);
				return result;
			}).onRollback(() -> null).execute();
		});
	}
	
	@Override
	public CentralisedFuture<Boolean> undoPunishmentById(final int id) {
		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			final long currentTime = MiscUtil.currentTime();
			return database.jdbCaesar().transaction().transactor((querySource) -> {

				for (PunishmentType type : MiscUtil.punishmentTypesExcludingKick()) {

					boolean deleted = querySource.query(
							"DELETE FROM `libertybans_" + type.getLowercaseNamePlural() + "` WHERE `id` = ?")
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
				return false;
			}).onRollback(() -> false).execute();
		});
	}
	
	@Override
	public CentralisedFuture<Punishment> undoAndGetPunishmentById(final int id) {
		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			return database.jdbCaesar().transaction().transactor((querySource) -> {

				final long currentTime = MiscUtil.currentTime();
				for (PunishmentType type : MiscUtil.punishmentTypesExcludingKick()) {

					Victim victim = querySource.query(
							// MariaDB-Connector requires the primary key to be present for ResultSet#deleteRow
							"SELECT `id`, `victim`, `victim_type` FROM `libertybans_" + type.getLowercaseNamePlural() + "` "
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
									return new SecurePunishment(id, type, victim, database.getOperatorFromResult(resultSet),
											database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
											database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
								}).execute();
						logger.trace("result={} in undoAndGetPunishmentById", result);
						return result;
					}
				}
				return null;
			}).onRollback(() -> null).execute();
		});
	}
	
	private static void checkSingular(PunishmentType type) {
		Objects.requireNonNull(type, "type");
		if (!type.isSingular()) {
			throw new IllegalArgumentException(
					"undoPunishmentByTypeAndVictim may only be used for singular punishments, not " + type);
		}
	}
	
	@Override
	public CentralisedFuture<Boolean> undoPunishmentByTypeAndVictim(final PunishmentType type, final Victim victim) {
		Objects.requireNonNull(victim, "victim");
		checkSingular(type);

		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			final long currentTime = MiscUtil.currentTime();
			if (database.getVendor().hasDeleteFromJoin()) {
				
			}
			return database.jdbCaesar().transaction().transactor((querySource) -> {

				Integer id = querySource.query(
						"SELECT `id` FROM `libertybans_" + type.getLowercaseNamePlural() + "` "
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
			}).onRollback(() -> false).execute();
		});
	}

	@Override
	public CentralisedFuture<Punishment> undoAndGetPunishmentByTypeAndVictim(final PunishmentType type, final Victim victim) {
		Objects.requireNonNull(victim, "victim");
		checkSingular(type);

		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			final long currentTime = MiscUtil.currentTime();
			return database.jdbCaesar().transaction().transactor((querySource) -> {

				Integer id = querySource.query(
						"SELECT `id` FROM `libertybans_" + type.getLowercaseNamePlural() + "` "
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
							return new SecurePunishment(id, type,
									victim, database.getOperatorFromResult(resultSet),
									database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
									database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
						}).execute();
				logger.trace("result={} in undoAndGetPunishmentByTypeAndVictim", result);
				return result;
			}).onRollback(() -> null).execute();
		});
	}
	
}
