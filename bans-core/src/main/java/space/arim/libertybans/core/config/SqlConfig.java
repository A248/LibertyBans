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

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault.DefaultInteger;
import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.IntegerRange;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.core.database.DatabaseSettingsConfig;
import space.arim.libertybans.core.database.RefreshTaskRunnable;

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
		"- MariaDB: Requires MariaDB 10.6 or newer.",
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
public interface SqlConfig extends DatabaseSettingsConfig {

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
		@DefaultInteger(20)
		int expirationTimeSeconds();

		@ConfKey("expiration-semantics")
		@ConfComments({"How the expiration time should be used. EXPIRE_AFTER_ACCESS is the default,",
				"which means that a cached mute's expiration time will be reset each time it is used.",
				"",
				"Set this to EXPIRE_AFTER_WRITE if there is any program or other instance of LibertyBans",
				"which may modify mutes in the database outside of this instance of LibertyBans.",
				"For example, if you are using a third-party tool which can delete or add mutes,",
				"EXPIRE_AFTER_WRITE is the correct semantic.",
				"",
				"If you are using multi-instance synchronization, this option is set automatically",
				"and you do not need to configure it.",
				"",
				"EXPIRE_AFTER_WRITE will expire the cached mute after a fixed time has elapsed",
				"since the mute was fetched. With EXPIRE_AFTER_ACCESS, the expiration time",
				"will refresh each time the mute is used."})
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
		@ConfComments({"How frequently the database is polled for updates, in milliseconds.",
				"Usually the default setting of 4 seconds will be sufficiently responsive without querying the database too often",
				"If you want to increase responsiveness, lower this value. If you want to reduce database load, increase this value.",
				"",
				"This value MUST be less than 30 seconds."})
		@IntegerRange(min = 250L, max = RefreshTaskRunnable.MAX_POLL_RATE_MILLIS)
		@DefaultInteger(4000)
		long pollRateMillis();

		default boolean enabled() {
			return mode() == SyncMode.ANSI_SQL;
		}
	}

}
