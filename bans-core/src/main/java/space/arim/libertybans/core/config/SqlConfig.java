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

package space.arim.libertybans.core.config;

import java.time.Duration;
import java.util.Map;

import space.arim.libertybans.core.database.Vendor;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault.DefaultBoolean;
import space.arim.dazzleconf.annote.ConfDefault.DefaultInteger;
import space.arim.dazzleconf.annote.ConfDefault.DefaultMap;
import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;

@ConfHeader({
		"",
		"SQL Database settings",
		"",
		"For most servers, the default options here are perfect. Most users will not need to bother here.",
		"",
		"However, for servers wishing to use an external database, or for large servers wishing to tweak performance,",
		"further configuration is made here.",
		"",
		"Database version requirements:",
		"- MariaDB: Requires MariaDB 10.3 or newer.",
		"- MySQL: Requires MySQL 8.0 or newer.",
		"- PostgreSQL: Requires PostgreSQL 12 or newer.",
		"- CockroachDB: Requires the latest version. Support for this database is experimental.",
		"",
		"Older versions of these respective databases are not supported.",
		"",
		"Information on character sets and encoding:",
		"- MariaDB: UTF8 is configured automatically",
		"- MySQL: UTF8 is configured automatically",
		"- PostgreSQL: UTF8 is used for the client encoding. It may be necessary to configure the database collation to use UTF8.",
		"- CockroachDB: This database uses UTF8 regardless.",
		"",
		"Note well:",
		"To apply changes made here, use '/libertybans restart' or restart the server.",
		"Using '/libertybans reload' will NOT update your database settings!",
		"",
		"",
		""})
public interface SqlConfig {

	@ConfKey("rdms-vendor")
	@ConfComments({
			"What RDMS vendor will you be using?",
			"Available options:",
			"'HSQLDB' - Local HyperSQL database. No additional requirements.",
			"'MARIADB' - Requires an external MariaDB database. At least MariaDB 10.3 is required.",
			"'MYSQL' - Requires an external MySQL database. At least MySQL 8.0 is required.",
			"'POSTGRES' - Requires an external PostgreSQL database. At least PostgreSQL 12 is required.",
			"'COCKROACH' - Requires an external CockroachDB database. The latest CockroachDB is required. " +
					"Warning: this option is strictly experimental."})
	@DefaultString("HSQLDB")
	Vendor vendor(); // Sensitive name used in integration testing
	
	@ConfKey("connection-pool-size")
	@ConfComments({
			"",
			"How large should the connection pool be?",
			"A thread pool of similar size is derived from the connection pool size.",
			"For most servers, the default option is suitable."})
	@DefaultInteger(6)
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
		@DefaultInteger(14)
		int connectionTimeoutSeconds();
		
		@ConfKey("max-lifetime-minutes")
		@ConfComments({
				"How long, at maxium, should a connection in the pool last before having to be recreated?",
				"\"This value should be set for MariaDB or MySQL. HikariCP notes:",
				"\"It should be several seconds shorter than any database or infrastructure imposed connection time limit\""})
		@DefaultInteger(25)
		int maxLifetimeMinutes();
		
	}
	
	@ConfKey("auth-details")
	@SubSection
	@ConfComments("Authentication details for remote databases: used for MariaDB, MySQL, PostgreSQL, and CockroachDB.")
	AuthDetails authDetails();  // Sensitive name used in integration testing
	
	interface AuthDetails {
		
		@DefaultString("localhost")
		String host();
		
		@DefaultInteger(3306)
		int port();
		
		@DefaultString("bans")
		String database();
		
		@ConfKey("user")
		@DefaultString("defaultuser")
		String username();
		
		@DefaultString("defaultpass")
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
		@DefaultMap({
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
		@DefaultMap({
				"preparedStatementCacheQueries", "25"})
		Map<String, String> connectionProperties();

	}

	@ConfKey("use-traditional-jdbc-url")
	@ConfComments("Legacy option. Don't touch this unless you understand it or you're told to enable it.")
	@DefaultBoolean(false)
	boolean useTraditionalJdbcUrl();

	@ConfKey("mute-caching")
	@SubSection
	MuteCaching muteCaching();

	@ConfHeader({"All punishments are stored fully in the database, with one exception.",
			"Mutes are cached for a small period of time so that players' chatting does not ",
			"flood your database with queries.",
			"",
			"Note: It is likely you do not need to touch this."})
	interface MuteCaching {

		@ConfKey("expiration-time-seconds")
		@ConfComments({"How long it takes a mute to expire"})
		@DefaultInteger(60)
		int expirationTimeSeconds();

		@ConfKey("expiration-semantics")
		@ConfComments({"How the expiration time should be used. EXPIRE_AFTER_ACCESS is the default,",
				"which means that a cached mute's expiration time will be reset each time it is used.",
				"",
				"Set this to EXPIRE_AFTER_WRITE if there is any program or other instance of LibertyBans",
				"which may modify mutes in the database outside of this instance of LibertyBans.",
				"Examples: multi-proxy networks running multiple LibertyBans instances,",
				"setups where LibertyBans is installed on multiple backend servers,",
				"or using a third-party tool which can delete or add mutes.",
				"EXPIRE_AFTER_WRITE is the correct semantic in these cases.",
				"",
				"EXPIRE_AFTER_WRITE will expire the cached mute after a fixed time has elapsed",
				"since the mute was fetched (unlike with EXPIRE_AFTER_ACCESS, the expiration time",
				"will not refresh each time the mute is used)."})
		@DefaultString("EXPIRE_AFTER_ACCESS")
		ExpirationSemantic expirationSemantic();

		enum ExpirationSemantic {
			EXPIRE_AFTER_ACCESS,
			EXPIRE_AFTER_WRITE
		}
	}

	@SubSection
	Synchronization synchronization();

	@ConfHeader("Settings for synchronizing multiple LibertyBans instances.")
	interface Synchronization {

		@ConfComments({"Availalble synchronization options:",
				"NONE - no synchronization",
				"ANSI_SQL - uses your database to synchronize punishments (called ANSI_SQL because it uses standard SQL)",
				"Other options may be added in the future, upon feature request."})
		@DefaultString("NONE")
		SyncMode mode();

		enum SyncMode {
			NONE,
			ANSI_SQL
		}

		@ConfKey("poll-rate-millis")
		@DefaultInteger(4000)
		@ConfComments({"How frequently the database is polled for updates, in milliseconds.",
				"Usually the default setting of 4 seconds will be sufficiently responsive without querying the database too often",
				"If you want to increase responsiveness, lower this value. If you want to reduce database load, increase this value.",
				"",
				"This value MUST be less than 30 seconds."})
		long pollRateMillis();

		default boolean enabled() {
			return mode() == SyncMode.ANSI_SQL;
		}
	}

}
