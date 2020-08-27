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
package space.arim.libertybans.core.selector;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentSelection;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Scope;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.MiscUtil;
import space.arim.libertybans.core.SecurePunishment;
import space.arim.libertybans.core.database.Database;

class SelectionImpl extends SelectorImplGroup {

	SelectionImpl(Selector selector) {
		super(selector);
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
	
	private static Map.Entry<StringBuilder, Object[]> getPredication(PunishmentSelection selection) {
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
			builder.append("(`end` = 0 OR `end` > ?)");
			params.add(MiscUtil.currentTime());
		}
		return Map.entry(builder, params.toArray());
	}
	
	private static SecurePunishment fromResultSetAndSelection(Database database, ResultSet resultSet,
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
	
	private static Map.Entry<String, Object[]> getSelectionQuery(PunishmentSelection selection) {
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
	
	CentralisedFuture<Punishment> getFirstSpecificPunishment(PunishmentSelection selection) {
		if (selection.selectActiveOnly() && selection.getType() == PunishmentType.KICK) {
			// Kicks cannot possibly be active. They are all history
			return core().getFuturesFactory().completedFuture(null);
		}
		Database database = core().getDatabase();
		return database.selectAsync(() -> {
			Map.Entry<String, Object[]> query = getSelectionQuery(selection);
			return database.jdbCaesar().query(
					query.getKey())
					.params(query.getValue())
					.singleResult((resultSet) -> {
						return fromResultSetAndSelection(database, resultSet, selection);
					}).onError(() -> null).execute();
		});
	}
	
	CentralisedFuture<Set<Punishment>> getSpecificPunishments(PunishmentSelection selection) {
		if (selection.selectActiveOnly() && selection.getType() == PunishmentType.KICK) {
			// Kicks cannot possibly be active. They are all history
			return core().getFuturesFactory().completedFuture(Set.of());
		}
		Database database = core().getDatabase();
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

}
