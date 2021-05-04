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
package space.arim.libertybans.core.config;

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
		"However, for servers wishing to use MariaDB / MySQL, or for large servers wishing to tweak performance,",
		"further setup is required.",
		"",
		"Using MariaDB requires at least MySQL 5.7/8.0 or MariaDB 10.2. Older versions of MySQL/MariaDB are not supported.",
		"",
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
			"'HSQLDB' - Local HyperSQL database. No additional setup required.",
			"'MARIADB' - Requires a separate database, and at least MySQL 5.7/8.0 or MariaDB 10.2. "})
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
	@ConfComments({
		"MariaDB / MySQL authentication details",
		"These must be changed to the proper credentials in order to use MariaDB/MySQL"})
	AuthDetails authDetails();  // Sensitive name used in integration testing
	
	interface AuthDetails {
		
		@DefaultString("localhost")
		String host();
		
		@DefaultInteger(3306)
		int port();
		
		@DefaultString("bans")
		String database();
		
		@ConfKey("user")
		@DefaultString("username")
		String username();
		
		@DefaultString("defaultpass")
		String password();
		
	}
	
	@ConfKey("mariadb")
	@ConfComments("The values in this section only apply when using a MariaDB / MySQL database")
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
				"prepStmtCacheSqlLimit", "256"})
		Map<String, String> connectionProperties();
		
		@ConfKey("use-event-scheduler")
		@ConfComments({"Whether to take advantage of the event scheduler to setup an event to clean out expired punishments.",
			"You must also turn on your database's event scheduler in order to use this."})
		@DefaultBoolean(false)
		boolean useEventScheduler();
		
	}
	
	@ConfKey("use-traditional-jdbc-url")
	@ConfComments("Legacy option. Don't touch this unless you understand it or you're told to enable it.")
	@DefaultBoolean(false)
	boolean useTraditionalJdbcUrl();

	@ConfKey("mute-caching")
	@SubSection
	MuteCaching muteCaching();

	@ConfHeader({"LibertyBans stores all punishments fully in the database, with one exception.",
			"Mutes are cached for a small period of time so that players chatting does not ",
			"flood your database with queries.",
			"",
			"Note: It is possible, even likely, you do not need to touch this.",
			"Among the servers which do configure a MariaDB database, an even fewer share of them ",
			"will need to touch the mute cache settings."})
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
				"using a third-party tool which can delete or add mutes.",
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
	
}
