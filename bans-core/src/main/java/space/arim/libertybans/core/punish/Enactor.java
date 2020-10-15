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

import java.time.Duration;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.ServerScope;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.core.database.Database;

import space.arim.jdbcaesar.mapper.UpdateCountMapper;

class Enactor extends EnforcementCenterMember {

	Enactor(EnforcementCenter center) {
		super(center);
	}
	
	CentralisedFuture<Punishment> enactPunishment(DraftPunishment draftPunishment) {
		Database database = core().getDatabase();
		return database.selectAsync(() -> {

			final PunishmentType type = draftPunishment.getType();
			final Victim victim = draftPunishment.getVictim();
			final Operator operator = draftPunishment.getOperator();
			final String reason = draftPunishment.getReason();
			final ServerScope scope = draftPunishment.getScope();
			final Duration duration = draftPunishment.getDuration();
			final long start = MiscUtil.currentTime();
			final long end = (duration.isZero()) ? 0L : start + duration.toSeconds();
			MiscUtil.checkRange(start, end);

			return database.jdbCaesar().transaction().body((querySource, controller) -> {

				if (type != PunishmentType.KICK) {
					database.clearExpiredPunishments(querySource, type, MiscUtil.currentTime());
				}

				if (database.getVendor().useStoredRoutines()) {
					String enactmentProcedure = MiscUtil.getEnactmentProcedure(type);

					return querySource.query(
							"{CALL `libertybans_" + enactmentProcedure + "` (?, ?, ?, ?, ?, ?, ?)}")
							.params(victim, victim.getType().ordinal() + 1, operator, reason,
									scope, start, end)

							.singleResult((resultSet) -> {
								int id = resultSet.getInt("id");
								return center.createPunishment(id, type, victim, operator, reason, scope, start, end);

							}).execute();
				} else {
					int id = querySource.query(
							"INSERT INTO `libertybans_punishments` (`type`, `operator`, `reason`, `scope`, `start`, `end`) "
							+ "VALUES (?, ?, ?, ?, ?, ?)")
							.params(type, operator, draftPunishment.getReason(), scope, start, end)
							.updateGenKeys((updateCount, genKeys) ->  {
								if (!genKeys.next()) {
									throw new IllegalStateException("No punishment ID generated for insertion query");
								}
								return genKeys.getInt("id");
							}).execute();

					if (type != PunishmentType.KICK) { // Kicks are pure history, so they jump straight to the end

						String enactStatement = "%INSERT% INTO `libertybans_" + type.getLowercaseNamePlural() + "` "
								+ "(`id`, `victim`, `victim_type`) VALUES (?, ?, ?)";
						Object[] enactArgs = new Object[] {id, victim, victim.getType()};

						if (type.isSingular()) {
							enactStatement = enactStatement.replace("%INSERT%", "INSERT IGNORE");
							int updateCount = querySource.query(enactStatement).params(enactArgs)
									.updateCount(UpdateCountMapper.identity()).execute();
							if (updateCount == 0) {
								controller.rollback();
								return null;
							}
						} else {
							enactStatement = enactStatement.replace("%INSERT%", "INSERT");
							querySource.query(enactStatement).params(enactArgs).voidResult().execute();
						}
					}

					querySource.query(
							"INSERT INTO `libertybans_history` (`id`, `victim`, `victim_type`) VALUES (?, ?, ?)")
							.params(id, victim, victim.getType())
							.voidResult().execute();
					return center.createPunishment(id, type, victim, operator, reason,scope, start, end);
				}
			}).onError(() -> null).execute();
		});
	}

}
