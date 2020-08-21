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
import space.arim.jdbcaesar.transact.RollMeBackException;

public class Enactor implements PunishmentEnactor {
	
	private final LibertyBansCore core;
	
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

			if (database.getVendor().useEnactmentProcedures()) {
				String enactmentProcedure = MiscUtil.getEnactmentProcedure(type);

				return database.jdbCaesar().query(
						"{CALL `libertybans_" + enactmentProcedure + "` (?, ?, ?, ?, ?, ?, ?)}")
						.params(victim, victim.getType(), operator, reason,
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

					int updateCount = querySource.query(
							"INSERT IGNORE INTO `libertybans_" + type.getLowercaseNamePlural() + "` (`id`, `victim`, `victim_type`) "
									+ "VALUES (?, ?, ?)")
							.params(id, victim, victim.getType())
							.updateCount(UpdateCountMapper.identity()).execute();

					if (type.isSingular() && updateCount == 0) {
						throw new RollMeBackException();
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
	public CentralisedFuture<Boolean> undoPunishment(Punishment punishment) {
		MiscUtil.validate(punishment);
		PunishmentType type = punishment.getType();
		if (type == PunishmentType.KICK) {
			// Kicks are never active, they're pure history, so they can never be undone
			return core.getFuturesFactory().completedFuture(false);
		}
		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			return database.jdbCaesar().query(
					"DELETE FROM `libertybans_" + type.getLowercaseNamePlural()
					+ "` WHERE `id` = ? AND (`end` = 0 OR `end` > ?)")
					.params(punishment.getID(), MiscUtil.currentTime())
					.updateCount((updateCount) -> updateCount == 1)
					.onError(() -> false)
					.execute();
		});
	}
	
	@Override
	public CentralisedFuture<Boolean> undoPunishmentById(final int id) {
		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			return database.jdbCaesar().transaction().transactor((querySource) -> {
				final long currentTime = MiscUtil.currentTime();
				for (PunishmentType type : MiscUtil.punishmentTypes()) {
					if (type == PunishmentType.KICK) {
						continue;
					}
					boolean deleted = querySource.query(
							"DELETE FROM `libertybans_" + type.getLowercaseNamePlural()
									+ "` WHERE `id` = ? AND (`end` = 0 OR `end` > ?)")
							.params(id, currentTime)
							.updateCount((updateCount) -> updateCount == 1)
							.execute();
					if (deleted) {
						return true;
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
				for (PunishmentType type : MiscUtil.punishmentTypes()) {
					if (type == PunishmentType.KICK) {
						continue;
					}
					Punishment punishment = querySource.query(
							"SELECT `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end` FROM "
									+ "`libertybans_simple_" + type.getLowercaseNamePlural() + "` WHERE "
									+ "`id` = ?")
							.params(id)
							.singleResult((resultSet) -> {
								return new SecurePunishment(id, type,
										database.getVictimFromResult(resultSet), database.getOperatorFromResult(resultSet),
										database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
										database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
							}).execute();
					if (punishment != null) {
						boolean deleted = querySource.query(
								"DELETE FROM `libertybans_" + type.getLowercaseNamePlural()
										+ "` WHERE `id` = ? AND (`end` = 0 OR `end` > ?)")
								.params(id, currentTime)
								.updateCount((updateCount) -> updateCount == 1).execute();
						assert deleted : punishment;
						return punishment;
					}
				}
				return null;
			}).onRollback(() -> null).execute();
		});
	}
	
	@Override
	public CentralisedFuture<Boolean> undoPunishmentByTypeAndVictim(final PunishmentType type, final Victim victim) {
		Objects.requireNonNull(victim, "victim");
		if (!type.isSingular()) {
			throw new IllegalArgumentException("undoPunishmentByTypeAndVictim may only be used for bans and mutes, not " + type);
		}
		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			return database.jdbCaesar().query(
					"DELETE FROM `libertybans_" + type.getLowercaseNamePlural()
					+ "` WHERE `victim` = ? AND `victim_type` = ?")
					.params(victim, victim.getType())
					.updateCount((updateCount) -> updateCount == 1)
					.execute();
		});
	}

	@Override
	public CentralisedFuture<Punishment> undoAndGetPunishmentByTypeAndVictim(PunishmentType type, Victim victim) {
		Objects.requireNonNull(victim, "victim");
		if (!type.isSingular()) {
			throw new IllegalArgumentException("undoAndGetPunishmentByTypeAndVictim may only be used for bans and mutes, not " + type);
		}
		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			return database.jdbCaesar().transaction().transactor((querySource) -> {
				Punishment punishment = querySource.query(
						"SELECT `id`, `operator`, `reason`, `scope`, `start`, `end` FROM "
						+ "`libertybans_simple_" + type.getLowercaseNamePlural() + "` WHERE "
						+ "`victim` = ? AND `victim_type` = ?")
						.params(victim, victim.getType())
						.singleResult((resultSet) -> {
							return new SecurePunishment(resultSet.getInt("id"), type,
								victim, database.getOperatorFromResult(resultSet),
								database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
								database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
						}).execute();
				if (punishment == null) {
					return null;
				}
				boolean deleted = querySource.query(
						"DELETE FROM `libertybans_" + type.getLowercaseNamePlural()
						+ "` WHERE `victim` = ? AND `victim_type` = ?")
						.params(victim, victim.getType())
						.updateCount((updateCount) -> updateCount == 1)
						.execute();
				assert deleted : punishment;
				return punishment;
			}).onRollback(() -> null).execute();
		});
	}

	@Override
	public CentralisedFuture<Boolean> undoWarnByIdAndVictim(int id, Victim victim) {
		Objects.requireNonNull(victim, "victim");
		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			return database.jdbCaesar().query(
					"DELETE FROM `libertybans_warns` WHERE `id` = ? AND `victim` = ? AND `victim_type` = ?")
					.params(id, victim, victim.getType())
					.updateCount((updateCount) -> updateCount == 1)
					.execute();
		});
	}

	@Override
	public CentralisedFuture<Punishment> undoAndGetWarnByIdAndVictim(int id, Victim victim) {
		Objects.requireNonNull(victim, "victim");
		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			return database.jdbCaesar().transaction().transactor((querySource) -> {
				Punishment punishment = querySource.query(
						"SELECT `operator`, `reason`, `scope`, `start`, `end` FROM "
						+ "`libertybans_simple_warns` WHERE `id` = ? AND `victim` = ? AND `victim_type` = ?")
						.params(id, victim, victim.getType())
						.singleResult((resultSet) -> {
							return new SecurePunishment(id, PunishmentType.WARN, victim, database.getOperatorFromResult(resultSet),
									database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
									database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
						}).execute();
				if (punishment == null) {
					return null;
				}
				boolean deleted = database.jdbCaesar().query(
						"DELETE FROM `libertybans_warns` WHERE `id` = ?")
						.params(id)
						.updateCount((updateCount) -> updateCount == 1)
						.execute();
				assert deleted : punishment;
				return punishment;
			}).onRollback(() -> null).execute();
		});
	}
	
}
