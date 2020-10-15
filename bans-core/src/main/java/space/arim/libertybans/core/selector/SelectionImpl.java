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
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.ServerScope;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.SelectionOrder;
import space.arim.libertybans.core.database.Database;
import space.arim.libertybans.core.punish.MiscUtil;

class SelectionImpl extends SelectorImplMember {

	SelectionImpl(Selector selector) {
		super(selector);
	}
	
	private static String getColumns(SelectionOrder selection) {
		List<String> columns = new ArrayList<>();
		columns.add("`id`");
		if (selection.getType() == null) {
			columns.add("`type`");
		}
		if (selection.getVictim() == null) {
			columns.add("`victim`");
			columns.add("`victim_type`");
		}
		if (selection.getOperator() == null) {
			columns.add("`operator`");
		}
		columns.add("`reason`");
		if (selection.getScope() == null) {
			columns.add("`scope`");
		}
		columns.add("`start`");
		columns.add("`end`");

		return String.join(", ", columns);
	}
	
	private static Map.Entry<String, Object[]> getPredication(SelectionOrder selection) {
		List<String> predicates = new ArrayList<>();
		List<Object> params = new ArrayList<>();

		if (selection.getType() != null) {
			predicates.add("`type` = ?");
			params.add(selection.getType());
		}
		if (selection.getVictim() != null) {
			predicates.add("`victim` = ? AND `victim_type` = ?");
			Victim victim = selection.getVictim();
			params.add(victim);
			params.add(victim.getType());
		}
		if (selection.getOperator() != null) {
			predicates.add("`operator` = ?");
			params.add(selection.getOperator());
		}
		if (selection.getScope() != null) {
			predicates.add("`scope` = ?");
			params.add(selection.getScope());
		}
		if (selection.selectActiveOnly()) {
			predicates.add("(`end` = 0 OR `end` > ?)");
			params.add(MiscUtil.currentTime());
		}
		return Map.entry(String.join(" AND ", predicates), params.toArray());
	}
	
	private Punishment fromResultSetAndSelection(Database database, ResultSet resultSet,
			SelectionOrder selection) throws SQLException {
		PunishmentType type = selection.getType();
		Victim victim = selection.getVictim();
		Operator operator = selection.getOperator();
		ServerScope scope = selection.getScope();
		return core().getEnforcementCenter().createPunishment(
				resultSet.getInt("id"),
				(type == null) ? database.getTypeFromResult(resultSet) : type,
				(victim == null) ? database.getVictimFromResult(resultSet) : victim,
				(operator == null) ? database.getOperatorFromResult(resultSet) : operator,
				database.getReasonFromResult(resultSet),
				(scope == null) ? database.getScopeFromResult(resultSet) : scope,
				database.getStartFromResult(resultSet),
				database.getEndFromResult(resultSet));
	}
	
	private static Map.Entry<String, Object[]> getSelectionQuery(SelectionOrder selection) {
		String columns = getColumns(selection);
		Map.Entry<String, Object[]> predication = getPredication(selection);

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

		String predicates = predication.getKey();
		if (!predicates.isEmpty()) {
			statementBuilder.append(" WHERE ").append(predicates);
		}
		return Map.entry(statementBuilder.toString(), predication.getValue());
	}
	
	CentralisedFuture<Punishment> getFirstSpecificPunishment(SelectionOrder selection) {
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
	
	CentralisedFuture<Set<Punishment>> getSpecificPunishments(SelectionOrder selection) {
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
						return fromResultSetAndSelection(database, resultSet, selection);
					}).onError(Set::of).execute();
			return Set.copyOf(result);
		});
	}

}
