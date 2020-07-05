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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import space.arim.universal.util.ThisClass;
import space.arim.universal.util.concurrent.CentralisedFuture;

import space.arim.uuidvault.api.UUIDUtil;

import space.arim.api.util.sql.MultiQueryResult;
import space.arim.api.util.sql.SqlQuery;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentSelection;
import space.arim.libertybans.api.PunishmentSelector;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Scope;
import space.arim.libertybans.api.Victim;

public class Selector implements PunishmentSelector, Part {

	private final LibertyBansCore core;
	
	private final AsyncLoadingCache<Object, Punishment> muteCache;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	Selector(LibertyBansCore core) {
		this.core = core;
		muteCache = Caffeine.newBuilder().expireAfterWrite(3L, TimeUnit.MINUTES).buildAsync((target, executor) -> {
			byte[][] targetInfo = (byte[][]) target;
			return getApplicablePunishment0(targetInfo[0], targetInfo[1], PunishmentType.MUTE);
		});
	}
	
	@Override
	public void startup() {
		//muteCache.synchronous().invalidateAll();
	}

	private static StringBuilder getColumns(PunishmentSelection selection) {
		List<String> columns = new ArrayList<>();
		columns.add("id");
		if (selection.getType() == null) {
			columns.add("type");
		}
		if (selection.getVictim() == null) {
			columns.add("victim");
			columns.add("victim_type");
		}
		if (selection.getOperator() == null) {
			columns.add("operator");
		}
		columns.add("reason");
		if (selection.getScope() == null) {
			columns.add("scope");
		}
		columns.add("start");
		columns.add("end");
		if (!selection.isSelectOnlyActive()) {
			columns.add("undone");
		}
		StringBuilder builder = new StringBuilder();
		String[] columnArray = columns.toArray(new String[] {});
		for (int n = 0; n < columnArray.length; n++) {
			if (n != 0) {
				builder.append(", ");
			}
			builder.append('`').append(columnArray[n]).append('`');
		}
		return builder;
	}
	
	private Entry<StringBuilder, Object[]> getPredication(PunishmentSelection selection) {
		boolean foundAny = false;
		List<Object> params = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		if (selection.getType() != null) {
			builder.append("`type` = ?");
			params.add(selection.getType().name());
			if (!foundAny) {
				foundAny = true;
			}
		}
		if (selection.getVictim() == null) {
			if (foundAny) {
				builder.append(" AND ");
			} else {
				foundAny = true;
			}
			builder.append("`victim` = ? AND `victim_type` = ?");
			Victim victim = selection.getVictim();
			params.add(core.getEnactor().getVictimBytes(victim));
			params.add(victim.getType().name());
		}
		if (selection.getOperator() == null) {
			if (foundAny) {
				builder.append(" AND ");
			} else {
				foundAny = true;
			}
			builder.append("`operator` = ?");
			params.add(core.getEnactor().getOperatorBytes(selection.getOperator()));
		}
		if (selection.getScope() == null) {
			if (foundAny) {
				builder.append(" AND ");
			} else {
				foundAny = true;
			}
			builder.append("`scope` = ?");
			params.add(core.getScopeManager().getServer(selection.getScope()));
		}
		if (!selection.isSelectOnlyActive()) {
			if (foundAny) {
				builder.append(" AND ");
			} else {
				foundAny = true;
			}
			builder.append("`undone` = FALSE AND (`end` = -1 OR `end` > ?)");
			params.add(System.currentTimeMillis());
		}
		return new AbstractMap.SimpleImmutableEntry<>(builder, params.toArray(new Object[] {}));
	}
	
	private SecurePunishment fromResultSetAndSelection(ResultSet rs, PunishmentSelection selection) throws SQLException {
		Enactor enactor = core.getEnactor();
		PunishmentType type = selection.getType();
		Victim victim = selection.getVictim();
		Operator operator = selection.getOperator();
		Scope scope = selection.getScope();
		boolean isActiveOnly = selection.isSelectOnlyActive();
		return new SecurePunishment(rs.getInt("id"), (type == null) ? enactor.getTypeFromResult(rs) : type,
				(victim == null) ? enactor.getVictimFromResult(rs) : victim,
				(operator == null) ? enactor.getOperatorFromResult(rs) : operator, enactor.getReasonFromResult(rs),
				(scope == null) ? enactor.getScopeFromResult(rs) : scope, enactor.getStartFromResult(rs),
				enactor.getEndFromResult(rs), !isActiveOnly && rs.getBoolean("undone"));
	}
	
	private SqlQuery getSelectionQuery(PunishmentSelection selection) {
		StringBuilder columns = getColumns(selection);
		Entry<StringBuilder, Object[]> predication = getPredication(selection);
		StringBuilder statementBuilder = new StringBuilder("SELECT ");
		statementBuilder.append(columns).append(" FROM `libertybans_punishments_");
		PunishmentType type = selection.getType();
		statementBuilder.append((type == null) ? "all" : type.getSingularity()).append('`');
		String predicates = predication.getKey().toString();
		if (!predicates.isEmpty()) {
			statementBuilder.append(" WHERE ").append(predicates);
		}
		return SqlQuery.of(statementBuilder.toString(), predication.getValue());
	}
	
	@Override
	public CentralisedFuture<Punishment> getFirstSpecificPunishment(PunishmentSelection selection) {
		return core.getDatabase().selectAsync(() -> {
			SqlQuery query = getSelectionQuery(selection);
			try (ResultSet rs = core.getDatabase().getBackend().select(query.getStatement(), query.getArgs())) {
				if (rs.next()) {
					return fromResultSetAndSelection(rs, selection);
				}
			} catch (SQLException ex) {
				logger.warn("Error selecting generic punishment selection {}", selection, ex);
			}
			return null;
		});
	}
	
	@Override
	public CentralisedFuture<Set<Punishment>> getSpecificPunishments(PunishmentSelection selection) {
		return core.getDatabase().selectAsync(() -> {
			Set<Punishment> result = new HashSet<>();
			SqlQuery query = getSelectionQuery(selection);
			try (ResultSet rs = core.getDatabase().getBackend().select(query.getStatement(), query.getArgs())) {
				while (rs.next()) {
					result.add(fromResultSetAndSelection(rs, selection));
				}
			} catch (SQLException ex) {
				logger.warn("Error selecting generic punishment selection {}", selection, ex);
			}
			return result;
		});
	}
	
	private SqlQuery getApplicabilityQuery(byte[] rawUUID, byte[] address, PunishmentType type) {
		String tableView = "`libertybans_applicable_" + type.getSingularity() + '`';
		String statement;
		Object[] args;
		if (core.getConfigs().strictAddressQueries()) {
			statement = "SELECT `id`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end`, `uuid` "
					+ "FROM " + tableView + " WHERE `type` = ? AND `uuid` = ?";
			args = new Object[] {type, rawUUID};
		} else {
			statement = "SELECT `id`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end`, `uuid` "
					+ "FROM " + tableView + " WHERE `type` = ? AND `uuid` = ? AND `address` = ?";
			args = new Object[] {type, rawUUID, address};
		}
		return SqlQuery.of(statement, args);
	}
	
	public CentralisedFuture<Punishment> executeAndCheckConnection(UUID uuid, String name, byte[] address) {
		return core.getDatabase().selectAsync(() -> {
			Enactor enactor = core.getEnactor();

			byte[] rawUUID = UUIDUtil.toByteArray(uuid);
			SqlQuery query = getApplicabilityQuery(rawUUID, address, PunishmentType.BAN);
			long currentTime = System.currentTimeMillis();
			try (MultiQueryResult mqr = core.getDatabase().getBackend().query(
					SqlQuery.of("INSERT INTO `libertybans_addresses` (`uuid`, `address`) VALUES (?, ?) "
							+ "ON DUPLICATE KEY UPDATE `updated` = ?", rawUUID, address, currentTime),
					SqlQuery.of("INSERT INTO `libertybans_names` (`uuid`, `name`) VALUES (?, ?) "
							+ "ON DUPLICATE KEY UPDATE `updated` = ?", rawUUID, name, currentTime),
					query)) {
				ResultSet rs = mqr.get(2).toResultSet();
				if (rs.next()) {
					return new SecurePunishment(rs.getInt("id"), PunishmentType.BAN, enactor.getVictimFromResult(rs), enactor.getOperatorFromResult(rs),
							enactor.getReasonFromResult(rs), enactor.getScopeFromResult(rs), enactor.getStartFromResult(rs),
							enactor.getEndFromResult(rs), false);
				}
			} catch (SQLException ex) {
				logger.warn("Unable to execute connection query for UUID {} and address {}",
						UUIDUtil.fromByteArray(rawUUID), address, ex);
			}
			return null;
		});
	}
	
	private CentralisedFuture<Punishment> getApplicablePunishment0(byte[] rawUUID, byte[] address, PunishmentType type) {
		return core.getDatabase().selectAsync(() -> {
			Enactor enactor = core.getEnactor();

			SqlQuery query = getApplicabilityQuery(rawUUID, address, type);
			try (ResultSet rs = core.getDatabase().getBackend().select(query.getStatement(), query.getArgs())) {
				if (rs.next()) {
					return new SecurePunishment(rs.getInt("id"), type, enactor.getVictimFromResult(rs), enactor.getOperatorFromResult(rs),
							enactor.getReasonFromResult(rs), enactor.getScopeFromResult(rs), enactor.getStartFromResult(rs),
							enactor.getEndFromResult(rs), false);
				}
			} catch (SQLException ex) {
				logger.warn("Unable to determine punishment {} for UUID {} and address {}",
						type, UUIDUtil.fromByteArray(rawUUID), address, ex);
			}
			return null;
		});
	}

	@Override
	public CentralisedFuture<Punishment> getApplicablePunishment(UUID uuid, byte[] address, PunishmentType type) {
		return getApplicablePunishment0(UUIDUtil.toByteArray(uuid), address, type);
	}

	@Override
	public CentralisedFuture<Set<Punishment>> getAllPunishmentsForVictim(Victim victim) {
		return core.getDatabase().selectAsync(() -> {
			Enactor enactor = core.getEnactor();
			byte[] victimBytes = enactor.getVictimBytes(victim);
			Set<Punishment> result = new HashSet<>();
			try (ResultSet rs = core.getDatabase().getBackend().select(
					"SELECT `id`, `type`, `operator`, `reason`, `scope`, `start`, `end`, `undone` FROM "
							+ "`libertybans_punishments_all` WHERE `victim` = ? AND `victim_type` = ?",
							victimBytes, victim.getType().name())) {
				while (rs.next()) {
					result.add(new SecurePunishment(rs.getInt("id"), enactor.getTypeFromResult(rs), victim,
							enactor.getOperatorFromResult(rs), enactor.getReasonFromResult(rs),
							enactor.getScopeFromResult(rs), enactor.getStartFromResult(rs),
							enactor.getEndFromResult(rs), rs.getBoolean("undone")));
				}
			} catch (SQLException ex) {
				logger.warn("Failed getting punishments for victim {}", victim, ex);
			}
			return result;
		});
	}

	@Override
	public CentralisedFuture<Set<Punishment>> getApplicablePunishmentsForType(PunishmentType type) {
		return core.getDatabase().selectAsync(() -> {
			Enactor enactor = core.getEnactor();
			String table = "`libertybans_punishments_" + type.getSingularity() + '`';
			Set<Punishment> result = new HashSet<>();
			try (ResultSet rs = core.getDatabase().getBackend().select(
					"SELECT `id`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end` FROM "
							+ table + " WHERE `type` = ? AND `undone` = FALSE AND (`end` = -1 OR `end` > ?)",
							type.name(), System.currentTimeMillis())) {
				while (rs.next()) {
					result.add(new SecurePunishment(rs.getInt("id"), type, enactor.getVictimFromResult(rs),
							enactor.getOperatorFromResult(rs), enactor.getReasonFromResult(rs),
							enactor.getScopeFromResult(rs), enactor.getStartFromResult(rs),
							enactor.getEndFromResult(rs), false));
				}
			} catch (SQLException ex) {
				logger.warn("Failed getting punishments for type {}", type, ex);
			}
			return result;
		});
	}

	@Override
	public void shutdown() {
	}

	@Override
	public CentralisedFuture<Punishment> getCachedMute(UUID uuid, byte[] address) {
		byte[][] targetInfo = new byte[2][];
		targetInfo[0] = UUIDUtil.toByteArray(uuid);
		targetInfo[1] = address;
		return (CentralisedFuture<Punishment>) muteCache.get(targetInfo);
	}

}
