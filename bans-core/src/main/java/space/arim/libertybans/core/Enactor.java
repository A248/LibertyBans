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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.universal.util.ThisClass;
import space.arim.universal.util.concurrent.CentralisedFuture;

import space.arim.uuidvault.api.UUIDUtil;

import space.arim.api.util.sql.QueryResult;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.DraftPunishment;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentEnactor;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Scope;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.Victim.VictimType;
import space.arim.libertybans.core.Scoper.ScopeImpl;

class Enactor implements PunishmentEnactor {
	
	private final LibertyBansCore core;
	
	private static final byte[] consoleUUIDBytes = UUIDUtil.toByteArray(new UUID(0, 0));
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	Enactor(LibertyBansCore core) {
		this.core = core;
	}

	CentralisedFuture<Punishment> enactPunishment(DraftPunishment draftPunishment, boolean insertIgnore) {
		return core.getDatabase().selectAsync(() -> {

			Victim victim = draftPunishment.getVictim();
			byte[] victimBytes = getVictimBytes(victim);

			Operator operator = draftPunishment.getOperator();
			byte[] operatorBytes = getOperatorBytes(operator);

			if (!(draftPunishment.getScope() instanceof ScopeImpl)) {
				throw new IllegalStateException("Foreign implementation of Scope: " + draftPunishment.getScope());
			}
			String server = core.getScopeManager().getServer(draftPunishment.getScope());

			try (QueryResult qr = core.getDatabase().getBackend().query(
					"INSERT " + ((insertIgnore) ? "IGNORE " : "") + "INTO `libertybans_punishments` "
					+ "(`type`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end`, `undone`) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
					draftPunishment.getType().name(), victimBytes, victim.getType().name(), operatorBytes,
					draftPunishment.getReason(), server, draftPunishment.getStart(), draftPunishment.getEnd(), false)) {

				ResultSet genKeys = qr.toUpdateResult().getGeneratedKeys();
				if (genKeys.next()) {
					int id = genKeys.getInt("id");
					return new SecurePunishment(id, draftPunishment.getType(), victim, operator,
							draftPunishment.getReason(), draftPunishment.getScope(), draftPunishment.getStart(),
							draftPunishment.getEnd(), false);
				}

			} catch (SQLException ex) {
				logger.error("Failed enacting punishment {}", draftPunishment);
			}
			return null;
		});
	}
	
	@Override
	public CentralisedFuture<Punishment> enactPunishment(DraftPunishment draftPunishment) {
		return enactPunishment(draftPunishment, false);
	}
	
	@Override
	public CentralisedFuture<Boolean> undoPunishment(Punishment punishment) {
		return core.getDatabase().selectAsync(() -> {
			String table = "`libertybans_punishments_" + ((punishment.getType().isSingular()) ? "singular" : "multiple") + '`';
			try (QueryResult qr = core.getDatabase().getBackend().query(
					"UPDATE " + table + " SET `undone` = 'TRUE' WHERE `id` = ?", punishment.getID())) {
				return qr.toUpdateResult().getUpdateCount() == 1;
			} catch (SQLException ex) {
				logger.warn("Failed to undo punishment {}", punishment);
			}
			return false;
		});
	}
	
	byte[] getVictimBytes(Victim victim) {
		VictimType vType = victim.getType();
		switch (vType) {
		case PLAYER:
			return UUIDUtil.toByteArray(((PlayerVictim) victim).getUUID());
		case ADDRESS:
			return ((AddressVictim) victim).getAddress();
		default:
			throw new IllegalStateException("Unknown VictimType " + vType);
		}
	}
	
	byte[] getOperatorBytes(Operator operator) {
		switch (operator.getType()) {
		case PLAYER:
			return UUIDUtil.toByteArray(((PlayerOperator) operator).getUUID());
		case CONSOLE:
			return consoleUUIDBytes;
		default:
			throw new IllegalStateException("Unknown OperatorType " + operator.getType());
		}
	}
	
	PunishmentType getTypeFromResult(ResultSet rs) throws SQLException {
		return PunishmentType.valueOf(rs.getString("type"));
	}
	
	Victim getVictimFromResult(ResultSet rs) throws SQLException {
		VictimType vType = VictimType.valueOf(rs.getString("victim_type"));
		byte[] bytes = rs.getBytes("victim");
		switch (vType) {
		case PLAYER:
			return PlayerVictim.of(UUIDUtil.fromByteArray(bytes));
		case ADDRESS:
			return AddressVictim.of(bytes);
		default:
			throw new IllegalStateException("Unknown VictimType: " + vType);
		}
	}
	
	Operator getOperatorFromResult(ResultSet rs) throws SQLException {
		byte[] operatorBytes = rs.getBytes("operator");
		if (Arrays.equals(operatorBytes, consoleUUIDBytes)) {
			return ConsoleOperator.INST;
		}
		return PlayerOperator.of(UUIDUtil.fromByteArray(operatorBytes));
	}
	
	String getReasonFromResult(ResultSet rs) throws SQLException {
		return rs.getString("reason");
	}

	Scope getScopeFromResult(ResultSet rs) throws SQLException {
		String server = rs.getString("scope");
		if (server != null) {
			return core.getScopeManager().specificScope(server);
		}
		return core.getScopeManager().globalScope();
	}
	
	long getStartFromResult(ResultSet rs) throws SQLException {
		return rs.getLong("start");
	}
	
	long getEndFromResult(ResultSet rs) throws SQLException {
		return rs.getLong("end");
	}
	
	@Override
	public void enforcePunishment(Punishment punishment) {
		core.getEnvironment().enforcePunishment(punishment);
	}

}
