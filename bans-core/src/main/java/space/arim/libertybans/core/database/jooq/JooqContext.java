/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

import org.jooq.*;
import org.jooq.conf.BackslashEscaping;
import org.jooq.conf.MappedSchema;
import org.jooq.conf.MappedTable;
import org.jooq.conf.RenderMapping;
import org.jooq.conf.Settings;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.NoConnectionProvider;

import java.sql.Connection;
import java.util.Objects;
import java.util.regex.Pattern;

import static space.arim.libertybans.core.database.DatabaseConstants.TABLE_PREFIX;

public final class JooqContext {

	static {
		// Silence JOOQ warnings about our database version
		System.setProperty(
				"org.jooq.log.org.jooq.impl.DefaultExecuteContext.logVersionSupport",
				Log.Level.ERROR.name()
		);
	}

	private final SQLDialect dialect;
	private final boolean retroSupport;

	public JooqContext(SQLDialect dialect, boolean retroSupport) {
		this.dialect = Objects.requireNonNull(dialect, "dialect");
		this.retroSupport = retroSupport;
	}

	public JooqContext(SQLDialect dialect) {
		this(dialect, false);
	}

	private static final Pattern MATCH_ALL_EXCEPT_INFORMATION_SCHEMA = Pattern.compile("^(?!INFORMATION_SCHEMA)(.*?)$");
	static final Pattern MATCH_ALL = Pattern.compile("^(.*?)$");
	static final String REPLACEMENT_ADDS_PREFIX = TABLE_PREFIX + "$0";

	public DSLContext createContext(Connection connection) {
		record SimpleConnectionProvider(Connection connection) implements ConnectionProvider {

			@Override
			public Connection acquire() throws DataAccessException {
				return connection;
			}

			@Override
			public void release(Connection connection) throws DataAccessException {}
		}
		return createWith(new SimpleConnectionProvider(connection));
	}

	public DSLContext createRenderOnlyContext() {
		return createWith(new NoConnectionProvider());
	}

	private DSLContext createWith(ConnectionProvider connectionProvider) {
		return new DefaultConfiguration()
				.set(connectionProvider)
				.set(dialect)
				.set(createSettings())
				.set(retroSupport ?
						new ExecuteListenerProvider[] { new RetroSupportListener().new Provider() }
						: new ExecuteListenerProvider[0]
				)
				.dsl();
	}

	private Settings createSettings() {
		return new Settings()
				.withBackslashEscaping(BackslashEscaping.OFF)
				.withRenderSchema(false)
				.withRenderMapping(new RenderMapping()
						.withSchemata(new MappedSchema()
								.withInputExpression(MATCH_ALL_EXCEPT_INFORMATION_SCHEMA)
								.withTables(new MappedTable()
										.withInputExpression(MATCH_ALL)
										.withOutput(REPLACEMENT_ADDS_PREFIX)
								)
						)
				)
				.withTransformPatternsUnnecessaryDistinct(false)
				.withTransformPatternsUnnecessaryExistsSubqueryClauses(false)
				.withTransformPatternsUnnecessaryGroupByExpressions(false)
				.withTransformPatternsUnnecessaryInnerJoin(false)
				.withTransformPatternsUnnecessaryOrderByExpressions(false)
				.withTransformPatternsUnnecessaryScalarSubquery(false);
	}

	@Override
	public String toString() {
		return "JooqContext{" +
				"dialect=" + dialect +
				", retroSupport=" + retroSupport +
				'}';
	}

}
