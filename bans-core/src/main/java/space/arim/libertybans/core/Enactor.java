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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.uuidvault.api.UUIDUtil;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.DraftPunishment;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentEnactor;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Scope;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.Victim.VictimType;
import space.arim.libertybans.core.database.CorruptDbException;
import space.arim.libertybans.core.database.Database;
import space.arim.libertybans.core.database.JdbCaesarHelper;

public class Enactor implements PunishmentEnactor {
	
	private final LibertyBansCore core;
	
	Enactor(LibertyBansCore core) {
		this.core = core;
	}

	@Override
	public CentralisedFuture<Punishment> enactPunishment(DraftPunishment draftPunishment) {
		MiscUtil.validate(draftPunishment);
		Database helper = core.getDatabase();
		return helper.selectAsync(() -> {

			Victim victim = draftPunishment.getVictim();
			Operator operator = draftPunishment.getOperator();

			String server = core.getScopeManager().getServer(draftPunishment.getScope());

			String enactmentProcedure = MiscUtil.getEnactmentProcedure(draftPunishment.getType());

			return helper.jdbCaesar().query(
					"{CALL `libertybans_" + enactmentProcedure + "` (?, ?, ?, ?, ?, ?, ?)}")
					.params(victim, victim.getType(), operator, draftPunishment.getReason(),
							server, draftPunishment.getStart(), draftPunishment.getEnd())
					.singleResult((resultSet) -> {
						int id = resultSet.getInt("id");
						return new SecurePunishment(id, draftPunishment.getType(), victim, operator,
								draftPunishment.getReason(), draftPunishment.getScope(), draftPunishment.getStart(),
								draftPunishment.getEnd());
					}).onError(() -> null).execute();
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
		Database helper = core.getDatabase();
		return helper.selectAsync(() -> {
			return helper.jdbCaesar().query(
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
		Database helper = core.getDatabase();
		return helper.selectAsync(() -> {
			return helper.jdbCaesar().transaction().transactor((querySource) -> {
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
	public CentralisedFuture<Boolean> undoPunishmentByTypeAndVictim(final PunishmentType type, final Victim victim) {
		if (type != PunishmentType.BAN && type != PunishmentType.MUTE) {
			throw new IllegalArgumentException("undoPunishmentByTypeAndVictim may only be used for bans and mutes, not " + type);
		}
		Database helper = core.getDatabase();
		return helper.selectAsync(() -> {
			return helper.jdbCaesar().query(
					"DELETE FROM `libertybans_" + type.getLowercaseNamePlural()
					+ "` WHERE `victim` = ? AND `victim_type` = ?")
					.params(victim, victim.getType())
					.updateCount((updateCount) -> updateCount == 1)
					.execute();
		});
	}
	
	PunishmentType getTypeFromResult(ResultSet resultSet) throws SQLException {
		return PunishmentType.valueOf(resultSet.getString("type"));
	}
	
	Victim getVictimFromResult(ResultSet resultSet) throws SQLException {
		VictimType vType = VictimType.valueOf(resultSet.getString("victim_type"));
		byte[] bytes = resultSet.getBytes("victim");
		switch (vType) {
		case PLAYER:
			return PlayerVictim.of(UUIDUtil.fromByteArray(bytes));
		case ADDRESS:
			try {
				return AddressVictim.of(InetAddress.getByAddress(bytes));
			} catch (UnknownHostException ex) {
				throw new CorruptDbException("Cannot parse internal IP address " + Arrays.toString(bytes), ex);
			}
		default:
			throw new IllegalStateException("Unknown victim type " + vType);
		}
	}
	
	Operator getOperatorFromResult(ResultSet resultSet) throws SQLException {
		return JdbCaesarHelper.getOperatorFromResult(resultSet);
	}
	
	String getReasonFromResult(ResultSet resultSet) throws SQLException {
		return resultSet.getString("reason");
	}

	Scope getScopeFromResult(ResultSet resultSet) throws SQLException {
		String server = resultSet.getString("scope");
		if (server != null) {
			return core.getScopeManager().specificScope(server);
		}
		return core.getScopeManager().globalScope();
	}
	
	long getStartFromResult(ResultSet resultSet) throws SQLException {
		return resultSet.getLong("start");
	}
	
	long getEndFromResult(ResultSet resultSet) throws SQLException {
		return resultSet.getLong("end");
	}

}
