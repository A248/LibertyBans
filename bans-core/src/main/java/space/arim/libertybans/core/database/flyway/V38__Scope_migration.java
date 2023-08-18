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

package space.arim.libertybans.core.database.flyway;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.SQLDataType;
import space.arim.libertybans.core.database.jooq.BatchExecute;
import space.arim.libertybans.core.database.sql.ScopeIdSequenceValue;
import space.arim.libertybans.core.scope.SpecificServerScope;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static space.arim.libertybans.core.schema.tables.Punishments.PUNISHMENTS;

public final class V38__Scope_migration extends BaseJavaMigration {

	private static final int BATCH_SIZE = 400;

	@Override
	public void migrate(Context flywayContext) throws Exception {
		DSLContext context;
		{
			MigrationState migrationState = MigrationState.retrieveState(flywayContext);
			Connection connection = flywayContext.getConnection();
			context = migrationState.createJooqContext(connection);
		}

		record RewriteScope(long punishmentId, int scopeId) {}
		List<RewriteScope> scopesToRewrite = new ArrayList<>();

		Field<String> legacyScopeField = field(name("scope"), SQLDataType.VARCHAR(32));
		try (var cursor = context
				.select(PUNISHMENTS.ID, legacyScopeField)
				.from(PUNISHMENTS)
				.where(legacyScopeField.notEqual(""))
				.fetchSize(BATCH_SIZE)
				.fetchLazy()) {

			for (var record : cursor) {
				Integer scopeId = new ScopeIdSequenceValue(context)
						.retrieveScopeIdFieldReified(new SpecificServerScope(record.value2()));
				scopesToRewrite.add(new RewriteScope(record.value1(), scopeId));
			}
		}
		new BatchExecute<RewriteScope>(
				() -> context.batch(context
						.update(PUNISHMENTS)
						.set(PUNISHMENTS.ID, (Long) null)
						.set(PUNISHMENTS.SCOPE_ID, (Integer) null)
				),
				(batch, data) -> {
					return batch.bind(data.punishmentId, data.scopeId);
				}
		).execute(scopesToRewrite, BATCH_SIZE);
	}

}
