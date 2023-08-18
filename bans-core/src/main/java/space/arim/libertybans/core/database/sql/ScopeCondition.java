/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.core.database.sql;

import org.jooq.Condition;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.scope.InternalScopeManager;

import static org.jooq.impl.DSL.noCondition;

public record ScopeCondition(ScopeFields scopeFields, InternalScopeManager scopeManager)
		implements MultiFieldCriterion<ServerScope> {

	@Override
	public Condition matchesValue(ServerScope scope) {
		return scopeManager.deconstruct(scope, (type, value) -> {
			Condition typeMatches = scopeFields.scopeType().eq(type);
			Condition valueMatches = switch (type) {
				case GLOBAL -> noCondition();
				case SERVER, CATEGORY -> scopeFields.scope().eq(value);
			};
			return typeMatches.and(valueMatches);
		});
	}

}
