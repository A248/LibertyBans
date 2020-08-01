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
import java.util.Collections;
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

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

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
import space.arim.libertybans.core.database.Database;

public class Selector implements PunishmentSelector {

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
		if (selection.getVictim() != null) {
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
		if (selection.getOperator() != null) {
			if (foundAny) {
				builder.append(" AND ");
			} else {
				foundAny = true;
			}
			builder.append("`operator` = ?");
			params.add(core.getEnactor().getOperatorBytes(selection.getOperator()));
		}
		if (selection.getScope() != null) {
			if (foundAny) {
				builder.append(" AND ");
			} else {
				foundAny = true;
			}
			builder.append("`scope` = ?");
			params.add(core.getScopeManager().getServer(selection.getScope()));
		}
		if (selection.selectActiveOnly()) {
			if (foundAny) {
				builder.append(" AND ");
			}
			builder.append("(`end` = -1 OR `end` > ?)");
			params.add(MiscUtil.currentTime());
		}
		return new AbstractMap.SimpleImmutableEntry<>(builder, params.toArray());
	}
	
	private SecurePunishment fromResultSetAndSelection(ResultSet rs, PunishmentSelection selection) throws SQLException {
		Enactor enactor = core.getEnactor();
		PunishmentType type = selection.getType();
		Victim victim = selection.getVictim();
		Operator operator = selection.getOperator();
		Scope scope = selection.getScope();
		return new SecurePunishment(rs.getInt("id"), (type == null) ? enactor.getTypeFromResult(rs) : type,
				(victim == null) ? enactor.getVictimFromResult(rs) : victim,
				(operator == null) ? enactor.getOperatorFromResult(rs) : operator, enactor.getReasonFromResult(rs),
				(scope == null) ? enactor.getScopeFromResult(rs) : scope, enactor.getStartFromResult(rs),
				enactor.getEndFromResult(rs));
	}
	
	private SqlQuery getSelectionQuery(PunishmentSelection selection) {
		StringBuilder columns = getColumns(selection);
		Entry<StringBuilder, Object[]> predication = getPredication(selection);

		StringBuilder statementBuilder = new StringBuilder("SELECT ");
		statementBuilder.append(columns).append(" FROM `libertybans_");

		PunishmentType type = selection.getType();
		if (selection.selectActiveOnly()) {
			assert type != PunishmentType.KICK : type;
			statementBuilder.append("simple_").append(type.getLowercaseNamePlural());
		} else {
			statementBuilder.append("history");
		}
		statementBuilder.append('`');

		String predicates = predication.getKey().toString();
		if (!predicates.isEmpty()) {
			statementBuilder.append(" WHERE ").append(predicates);
		}
		return SqlQuery.of(statementBuilder.toString(), predication.getValue());
	}
	
	@Override
	public CentralisedFuture<Punishment> getFirstSpecificPunishment(PunishmentSelection selection) {
		if (selection.selectActiveOnly() && selection.getType() == PunishmentType.KICK) {
			// Kicks cannot possibly be active. They are all history
			return core.getFuturesFactory().completedFuture(null);
		}
		Database helper = core.getDatabase();
		return helper.selectAsync(() -> {
			SqlQuery query = getSelectionQuery(selection);
			try (ResultSet rs = helper.getBackend().select(query.getStatement(), query.getArgs())) {
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
		if (selection.selectActiveOnly() && selection.getType() == PunishmentType.KICK) {
			// Kicks cannot possibly be active. They are all history
			return core.getFuturesFactory().completedFuture(Set.of());
		}
		Database helper = core.getDatabase();
		return helper.selectAsync(() -> {
			Set<Punishment> result = new HashSet<>();
			SqlQuery query = getSelectionQuery(selection);
			try (ResultSet rs = helper.getBackend().select(query.getStatement(), query.getArgs())) {
				while (rs.next()) {
					result.add(fromResultSetAndSelection(rs, selection));
				}
			} catch (SQLException ex) {
				logger.warn("Error selecting generic punishment selection {}", selection, ex);
			}
			return Collections.unmodifiableSet(result);
		});
	}
	
	private SqlQuery getApplicabilityQuery(byte[] rawUUID, byte[] address, PunishmentType type) {
		String tableView = "`libertybans_applicable_" + type.getLowercaseNamePlural() + '`';
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
	
	/**
	 * Checks a player connection's in a single connection query, enforcing any applicable bans. <br>
	 * This is called by the environmental listeners.
	 * 
	 * @param uuid the player UUID
	 * @param name the player name
	 * @param address the player IP address
	 * @return a future which yields the ban itself, or null if there is none
	 */
	public CentralisedFuture<Punishment> executeAndCheckConnection(UUID uuid, String name, byte[] address) {
		core.getUUIDMaster().addCache(uuid, name);

		Database helper = core.getDatabase();
		return helper.selectAsync(() -> {
			Enactor enactor = core.getEnactor();

			byte[] rawUUID = UUIDUtil.toByteArray(uuid);
			SqlQuery query = getApplicabilityQuery(rawUUID, address, PunishmentType.BAN);
			long currentTime = MiscUtil.currentTime();
			try (MultiQueryResult mqr = helper.getBackend().query(
					SqlQuery.of("INSERT INTO `libertybans_addresses` (`uuid`, `address`) VALUES (?, ?) "
							+ "ON DUPLICATE KEY UPDATE `updated` = ?", rawUUID, address, currentTime),
					SqlQuery.of("INSERT INTO `libertybans_names` (`uuid`, `name`) VALUES (?, ?) "
							+ "ON DUPLICATE KEY UPDATE `updated` = ?", rawUUID, name, currentTime),
					query)) {
				ResultSet rs = mqr.get(2).toResultSet();
				if (rs.next()) {
					return new SecurePunishment(rs.getInt("id"), PunishmentType.BAN, enactor.getVictimFromResult(rs), enactor.getOperatorFromResult(rs),
							enactor.getReasonFromResult(rs), enactor.getScopeFromResult(rs), enactor.getStartFromResult(rs),
							enactor.getEndFromResult(rs));
				}
			} catch (SQLException ex) {
				logger.warn("Unable to execute connection query for UUID {} and address {}",
						UUIDUtil.fromByteArray(rawUUID), address, ex);
			}
			return null;
		});
	}
	
	private CentralisedFuture<Punishment> getApplicablePunishment0(byte[] rawUUID, byte[] address, PunishmentType type) {
		Database helper = core.getDatabase();
		return helper.selectAsync(() -> {
			Enactor enactor = core.getEnactor();

			SqlQuery query = getApplicabilityQuery(rawUUID, address, type);
			try (ResultSet rs = helper.getBackend().select(query.getStatement(), query.getArgs())) {
				if (rs.next()) {
					return new SecurePunishment(rs.getInt("id"), type, enactor.getVictimFromResult(rs), enactor.getOperatorFromResult(rs),
							enactor.getReasonFromResult(rs), enactor.getScopeFromResult(rs), enactor.getStartFromResult(rs),
							enactor.getEndFromResult(rs));
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
		if (type == PunishmentType.KICK) {
			// Kicks cannot possibly be active. They are all history
			return core.getFuturesFactory().completedFuture(null);
		}
		return getApplicablePunishment0(UUIDUtil.toByteArray(uuid), address, type);
	}

	@Override
	public CentralisedFuture<Set<Punishment>> getHistoryForVictim(Victim victim) {
		Database helper = core.getDatabase();
		return helper.selectAsync(() -> {
			Enactor enactor = core.getEnactor();
			byte[] victimBytes = enactor.getVictimBytes(victim);
			Set<Punishment> result = new HashSet<>();
			try (ResultSet rs = helper.getBackend().select(
					"SELECT `id`, `type`, `operator`, `reason`, `scope`, `start`, `end`, `undone` FROM "
							+ "`libertybans_history` WHERE `victim` = ? AND `victim_type` = ?",
							victimBytes, victim.getType().name())) {
				while (rs.next()) {
					result.add(new SecurePunishment(rs.getInt("id"), enactor.getTypeFromResult(rs), victim,
							enactor.getOperatorFromResult(rs), enactor.getReasonFromResult(rs),
							enactor.getScopeFromResult(rs), enactor.getStartFromResult(rs),
							enactor.getEndFromResult(rs)));
				}
			} catch (SQLException ex) {
				logger.warn("Failed getting punishments for victim {}", victim, ex);
			}
			return Collections.unmodifiableSet(result);
		});
	}

	@Override
	public CentralisedFuture<Set<Punishment>> getActivePunishmentsForType(PunishmentType type) {
		if (type == PunishmentType.KICK) {
			return core.getFuturesFactory().completedFuture(Set.of());
		}
		Database helper = core.getDatabase();
		return helper.selectAsync(() -> {
			Enactor enactor = core.getEnactor();
			String table = "`libertybans_" + type.getLowercaseNamePlural() + '`';
			Set<Punishment> result = new HashSet<>();
			try (ResultSet rs = helper.getBackend().select(
					"SELECT `id`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end` FROM "
							+ table + " WHERE `end` = -1 OR `end` > ?", MiscUtil.currentTime())) {

				while (rs.next()) {
					result.add(new SecurePunishment(rs.getInt("id"), type, enactor.getVictimFromResult(rs),
							enactor.getOperatorFromResult(rs), enactor.getReasonFromResult(rs),
							enactor.getScopeFromResult(rs), enactor.getStartFromResult(rs),
							enactor.getEndFromResult(rs)));
				}
			} catch (SQLException ex) {
				logger.warn("Failed getting active punishments for type {}", type, ex);
			}
			return Collections.unmodifiableSet(result);
		});
	}

	@Override
	public CentralisedFuture<Punishment> getCachedMute(UUID uuid, byte[] address) {
		byte[][] targetInfo = new byte[2][];
		targetInfo[0] = UUIDUtil.toByteArray(uuid);
		targetInfo[1] = address;
		return (CentralisedFuture<Punishment>) muteCache.get(targetInfo);
	}

}
