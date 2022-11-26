/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.core.database;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.core.config.SqlConfig;
import space.arim.libertybans.core.importing.ImportConfig;

import java.util.Map;

/**
 * Base interface for database connection configuration. Used by {@link SqlConfig} and {@link ImportConfig}
 *
 */
public interface DatabaseSettingsConfig {

	@ConfKey("rdms-vendor")
	@ConfComments({
			"What RDMS vendor will you be using?",
			"Available options:",
			"'HSQLDB' - Local HyperSQL database. No additional requirements.",
			"'MARIADB' - Requires an external MariaDB database. At least MariaDB 10.6 is required.",
			"'MYSQL' - Requires an external MySQL database. At least MySQL 8.0 is required.",
			"'POSTGRES' - Requires an external PostgreSQL database. At least PostgreSQL 12 is required.",
			"'COCKROACH' - Requires an external CockroachDB database. The latest CockroachDB is required. " +
					"Warning: this option is strictly experimental."})
	@ConfDefault.DefaultString("HSQLDB")
	Vendor vendor(); // Sensitive name used in integration testing

	@ConfKey("connection-pool-size")
	@ConfComments({
			"",
			"How large should the connection pool be?",
			"A thread pool of similar size is derived from the connection pool size.",
			"For most servers, the default option is suitable."})
	@ConfDefault.DefaultInteger(6)
	int poolSize();

	@SubSection
	@ConfComments({
			"",
			"Connection timeout settings",
			"LibertyBans uses HikariCP for connection pooling. The following settings control connection timeouts."})
	Timeouts timeouts();

	interface Timeouts {

		@ConfKey("connection-timeout-seconds")
		@ConfComments({
				"How long, at maximum, should LibertyBans wait when acquiring new connections, "
						+ "if no existing connection is available?"})
		@ConfDefault.DefaultInteger(14)
		int connectionTimeoutSeconds();

		@ConfKey("max-lifetime-minutes")
		@ConfComments({
				"How long, at maxium, should a connection in the pool last before having to be recreated?",
				"\"This value should be set for MariaDB or MySQL. HikariCP notes:",
				"\"It should be several seconds shorter than any database or infrastructure imposed connection time limit\""})
		@ConfDefault.DefaultInteger(25)
		int maxLifetimeMinutes();

	}

	@ConfKey("auth-details")
	@SubSection
	@ConfComments("Authentication details for remote databases: used for MariaDB, MySQL, PostgreSQL, and CockroachDB.")
	AuthDetails authDetails();  // Sensitive name used in integration testing

	interface AuthDetails {

		@ConfDefault.DefaultString("localhost")
		String host();

		@ConfDefault.DefaultInteger(3306)
		int port();

		@ConfDefault.DefaultString("bans")
		String database();

		@ConfKey("user")
		@ConfDefault.DefaultString("defaultuser")
		String username();

		@ConfDefault.DefaultString("defaultpass")
		String password();

	}

	@ConfKey("mariadb")
	@ConfComments("The values in this section only apply when using a MariaDB or MySQL database")
	@SubSection
	MariaDbConfig mariaDb();

	interface MariaDbConfig {

		@ConfKey("connection-properties")
		@ConfComments({
				"Connection properties to be applied to database connections"
		})
		@ConfDefault.DefaultMap({
				"useUnicode", "true",
				"characterEncoding", "UTF-8",
				"useServerPrepStmts", "true",
				"cachePrepStmts", "true",
				"prepStmtCacheSize", "25",
				"prepStmtCacheSqlLimit", "1024"})
		Map<String, String> connectionProperties();

	}

	@ConfKey("postgres")
	@ConfComments("The values in this section only apply when using a PostgreSQL or CockroachDB database")
	@SubSection
	PostgresConfig postgres();

	interface PostgresConfig {

		@ConfKey("connection-properties")
		@ConfComments("Connection properties to be applied to database connections")
		@ConfDefault.DefaultMap({
				"preparedStatementCacheQueries", "25"})
		Map<String, String> connectionProperties();

	}

	@ConfKey("use-traditional-jdbc-url")
	@ConfComments("Legacy option. Don't touch this unless you understand it or you're told to enable it.")
	@ConfDefault.DefaultBoolean(false)
	boolean useTraditionalJdbcUrl();

}
