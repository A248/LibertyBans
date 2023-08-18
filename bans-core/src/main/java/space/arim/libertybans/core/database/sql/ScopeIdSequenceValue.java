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

import org.jooq.DSLContext;
import org.jooq.Field;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.scope.ScopeParsing;
import space.arim.libertybans.core.scope.ScopeType;

import static org.jooq.impl.DSL.castNull;
import static org.jooq.impl.DSL.val;
import static space.arim.libertybans.core.schema.Sequences.LIBERTYBANS_SCOPE_IDS;
import static space.arim.libertybans.core.schema.tables.Scopes.SCOPES;

public final class ScopeIdSequenceValue extends SequenceValue<Integer> {

	public ScopeIdSequenceValue(DSLContext context) {
		super(context, LIBERTYBANS_SCOPE_IDS);
	}

	private RetrieveOrGenerate retrieveOrGenerate(ScopeType type, String value) {
		return new RetrieveOrGenerate(
				SCOPES, SCOPES.ID,
				SCOPES.TYPE.eq(type).and(SCOPES.VALUE.eq(value)),
				(newId) -> {
					context
							.insertInto(SCOPES)
							.columns(SCOPES.ID, SCOPES.TYPE, SCOPES.VALUE)
							.values(newId, val(type), val(value))
							.execute();
				}
		);
	}

	public Field<Integer> retrieveScopeId(ServerScope scope) {
		return new ScopeParsing().deconstruct(scope, (type, value) -> {
			if (type == ScopeType.GLOBAL) {
				return castNull(Integer.class);
			}
			return retrieveOrGenerate(type, value).execute();
		});
	}

	public Integer retrieveScopeIdFieldReified(ServerScope scope) {
		return new ScopeParsing().deconstruct(scope, (type, value) -> {
			if (type == ScopeType.GLOBAL) {
				return null;
			}
			return retrieveOrGenerate(type, value).executeReified();
		});
	}

}
