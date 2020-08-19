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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

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
	
	private final AsyncLoadingCache<MuteCacheKey, Punishment> muteCache;
	
	Selector(LibertyBansCore core) {
		this.core = core;
		muteCache = Caffeine.newBuilder().expireAfterWrite(3L, TimeUnit.MINUTES).buildAsync((target, executor) -> {
			return getApplicablePunishment0(target.uuid, target.address, PunishmentType.MUTE);
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
	
	private Map.Entry<StringBuilder, Object[]> getPredication(PunishmentSelection selection) {
		boolean foundAny = false;
		List<Object> params = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		if (selection.getType() != null) {
			builder.append("`type` = ?");
			params.add(selection.getType());
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
			params.add(victim);
			params.add(victim.getType());
		}
		if (selection.getOperator() != null) {
			if (foundAny) {
				builder.append(" AND ");
			} else {
				foundAny = true;
			}
			builder.append("`operator` = ?");
			params.add(selection.getOperator());
		}
		if (selection.getScope() != null) {
			if (foundAny) {
				builder.append(" AND ");
			} else {
				foundAny = true;
			}
			builder.append("`scope` = ?");
			params.add(selection.getScope());
		}
		if (selection.selectActiveOnly()) {
			if (foundAny) {
				builder.append(" AND ");
			}
			builder.append("(`end` = -1 OR `end` > ?)");
			params.add(MiscUtil.currentTime());
		}
		return Map.entry(builder, params.toArray());
	}
	
	private SecurePunishment fromResultSetAndSelection(Database database, ResultSet resultSet,
			PunishmentSelection selection) throws SQLException {
		PunishmentType type = selection.getType();
		Victim victim = selection.getVictim();
		Operator operator = selection.getOperator();
		Scope scope = selection.getScope();
		return new SecurePunishment(resultSet.getInt("id"),
				(type == null) ? database.getTypeFromResult(resultSet) : type,
				(victim == null) ? database.getVictimFromResult(resultSet) : victim,
				(operator == null) ? database.getOperatorFromResult(resultSet) : operator,
				database.getReasonFromResult(resultSet),
				(scope == null) ? database.getScopeFromResult(resultSet) : scope,
				database.getStartFromResult(resultSet),
				database.getEndFromResult(resultSet));
	}
	
	private Map.Entry<String, Object[]> getSelectionQuery(PunishmentSelection selection) {
		StringBuilder columns = getColumns(selection);
		Map.Entry<StringBuilder, Object[]> predication = getPredication(selection);

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
		return Map.entry(statementBuilder.toString(), predication.getValue());
	}
	
	@Override
	public CentralisedFuture<Punishment> getFirstSpecificPunishment(PunishmentSelection selection) {
		if (selection.selectActiveOnly() && selection.getType() == PunishmentType.KICK) {
			// Kicks cannot possibly be active. They are all history
			return core.getFuturesFactory().completedFuture(null);
		}
		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			Map.Entry<String, Object[]> query = getSelectionQuery(selection);
			return database.jdbCaesar().query(
					query.getKey())
					.params(query.getValue())
					.singleResult((resultSet) -> {
						return fromResultSetAndSelection(database, resultSet, selection);
					}).onError(() -> null)
					.execute();
		});
	}
	
	@Override
	public CentralisedFuture<Set<Punishment>> getSpecificPunishments(PunishmentSelection selection) {
		if (selection.selectActiveOnly() && selection.getType() == PunishmentType.KICK) {
			// Kicks cannot possibly be active. They are all history
			return core.getFuturesFactory().completedFuture(Set.of());
		}
		Database database = core.getDatabase();
		return database.selectAsync(() -> {
			Map.Entry<String, Object[]> query = getSelectionQuery(selection);
			Set<Punishment> result = database.jdbCaesar().query(
					query.getKey())
					.params(query.getValue())
					.setResult((resultSet) -> {
						return (Punishment) fromResultSetAndSelection(database, resultSet, selection);
					}).onError(Set::of).execute();
			return Set.copyOf(result);
		});
	}
	
	private Map.Entry<String, Object[]> getApplicabilityQuery(UUID uuid, byte[] address, PunishmentType type) {
		String tableView = "`libertybans_applicable_" + type.getLowercaseNamePlural() + '`';
		String statement;
		Object[] args;
		if (core.getConfigs().strictAddressQueries()) {
			statement = "SELECT `id`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end`, `uuid` "
					+ "FROM " + tableView + " WHERE `type` = ? AND `uuid` = ?";
			args = new Object[] {type, uuid};
		} else {
			statement = "SELECT `id`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end`, `uuid` "
					+ "FROM " + tableView + " WHERE `type` = ? AND `uuid` = ? AND `address` = ?";
			args = new Object[] {type, uuid, address};
		}
		return Map.entry(statement, args);
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

		Database database = core.getDatabase();
		return database.selectAsync(() -> {

			Map.Entry<String, Object[]> query = getApplicabilityQuery(uuid, address, PunishmentType.BAN);
			long currentTime = MiscUtil.currentTime();

			return database.jdbCaesar().transaction().transactor((querySource) -> {
				querySource.query(
						"INSERT INTO `libertybans_addresses` (`uuid`, `address`) VALUES (?, ?) "
								+ "ON DUPLICATE KEY UPDATE `updated` = ?")
						.params(uuid, address, currentTime)
						.voidResult().execute();
				querySource.query(
						"INSERT INTO `libertybans_names` (`uuid`, `name`) VALUES (?, ?) "
						+ "ON DUPLICATE KEY UPDATE `updated` = ?")
						.params(uuid, name, currentTime)
						.voidResult().execute();
				Punishment potentialBan = querySource.query(
						query.getKey())
						.params(query.getValue())
						.singleResult((resultSet) -> {
							return new SecurePunishment(resultSet.getInt("id"), PunishmentType.BAN,
									database.getVictimFromResult(resultSet), database.getOperatorFromResult(resultSet),
									database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
									database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
						}).execute();
				return potentialBan;
			}).onRollback(() -> null).execute();
		});
	}
	
	private CentralisedFuture<Punishment> getApplicablePunishment0(UUID uuid, byte[] address, PunishmentType type) {
		Database database = core.getDatabase();
		return database.selectAsync(() -> {

			Map.Entry<String, Object[]> query = getApplicabilityQuery(uuid, address, type);
			return database.jdbCaesar().query(
					query.getKey())
					.params(query.getValue())
					.singleResult((resultSet) -> {
						return new SecurePunishment(resultSet.getInt("id"), type,
								database.getVictimFromResult(resultSet), database.getOperatorFromResult(resultSet),
								database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
								database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
					}).onError(() -> null).execute();
		});
	}

	@Override
	public CentralisedFuture<Punishment> getApplicablePunishment(UUID uuid, byte[] address, PunishmentType type) {
		if (type == PunishmentType.KICK) {
			// Kicks cannot possibly be active. They are all history
			return core.getFuturesFactory().completedFuture(null);
		}
		return getApplicablePunishment0(uuid, address, type);
	}

	@Override
	public CentralisedFuture<Set<Punishment>> getHistoryForVictim(Victim victim) {
		Database database = core.getDatabase();
		return database.selectAsync(() -> {

			Set<Punishment> result = database.jdbCaesar().query(
						"SELECT `id`, `type`, `operator`, `reason`, `scope`, `start`, `end`, `undone` FROM "
						+ "`libertybans_history` WHERE `victim` = ? AND `victim_type` = ?")
					.params(victim, victim.getType())
					.setResult((resultSet) -> {
						return (Punishment) new SecurePunishment(resultSet.getInt("id"),
								database.getTypeFromResult(resultSet), victim, database.getOperatorFromResult(resultSet),
								database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
								database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
					}).onError(Set::of).execute();
			return Set.copyOf(result);
		});
	}

	@Override
	public CentralisedFuture<Set<Punishment>> getActivePunishmentsForType(PunishmentType type) {
		if (type == PunishmentType.KICK) {
			return core.getFuturesFactory().completedFuture(Set.of());
		}
		Database database = core.getDatabase();
		return database.selectAsync(() -> {

			String table = "`libertybans_" + type.getLowercaseNamePlural() + '`';
			Set<Punishment> result = database.jdbCaesar().query(
					"SELECT `id`, `victim`, `victim_type`, `operator`, `reason`, `scope`, `start`, `end` FROM "
					+ table + " WHERE `end` = -1 OR `end` > ?")
					.params(MiscUtil.currentTime())
					.setResult((resultSet) -> {
						return (Punishment) new SecurePunishment(resultSet.getInt("id"), type,
								database.getVictimFromResult(resultSet), database.getOperatorFromResult(resultSet),
								database.getReasonFromResult(resultSet), database.getScopeFromResult(resultSet),
								database.getStartFromResult(resultSet), database.getEndFromResult(resultSet));
					}).onError(Set::of).execute();
			return Set.copyOf(result);
		});
	}

	@Override
	public CentralisedFuture<Punishment> getCachedMute(UUID uuid, byte[] address) {
		return (CentralisedFuture<Punishment>) muteCache.get(new MuteCacheKey(uuid, address));
	}
	
	private static class MuteCacheKey {
		
		final UUID uuid;
		final byte[] address;
		
		MuteCacheKey(UUID uuid, byte[] address) {
			this.uuid = uuid;
			this.address = address;
		}

		@Override
		public String toString() {
			return "MuteCacheKey [uuid=" + uuid + ", address=" + Arrays.toString(address) + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + uuid.hashCode();
			result = prime * result + Arrays.hashCode(address);
			return result;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (!(object instanceof MuteCacheKey)) {
				return false;
			}
			MuteCacheKey other = (MuteCacheKey) object;
			return uuid.equals(other.uuid) && Arrays.equals(address, other.address);
		}
		
	}

}
