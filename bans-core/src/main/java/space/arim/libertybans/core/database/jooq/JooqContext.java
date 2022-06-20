/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.core.database.jooq;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.MappedTable;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.util.Objects;
import java.util.regex.Pattern;

public final class JooqContext {

	private final SQLDialect dialect;

	public JooqContext(SQLDialect dialect) {
		this.dialect = Objects.requireNonNull(dialect, "dialect");
	}

	private static final Pattern MATCH_ALL_EXCEPT_INFORMATION_SCHEMA = Pattern.compile("^(?!INFORMATION_SCHEMA)(.*?)$");
	static final Pattern MATCH_ALL = Pattern.compile("^(.*?)$");
	static final String REPLACEMENT = "libertybans_$0";

	public DSLContext createContext(Connection connection) {
		return DSL.using(connection, dialect, createSettings());
	}

	public DSLContext createRenderOnlyContext() {
		return DSL.using(dialect, createSettings());
	}

	private Settings createSettings() {
		return new Settings()
				.withRenderSchema(false)
				.withRenderMapping(new RenderMapping()
						.withSchemata(new MappedSchema()
								.withInputExpression(MATCH_ALL_EXCEPT_INFORMATION_SCHEMA)
								.withTables(new MappedTable()
										.withInputExpression(MATCH_ALL)
										.withOutput(REPLACEMENT)
								)
						)
				);
	}

	@Override
	public String toString() {
		return "JooqContext{" +
				"dialect=" + dialect +
				'}';
	}
}
