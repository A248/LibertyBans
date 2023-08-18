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

package space.arim.libertybans.core.database;

import org.jooq.SQLDialect;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public enum Vendor {

	HSQLDB("HyperSQL", JdbcDriver.HSQLDB),
	MARIADB("MariaDB", JdbcDriver.MARIADB_CONNECTOR),
	MYSQL("MySQL", JdbcDriver.MARIADB_CONNECTOR),
	POSTGRES("PostgreSQL", JdbcDriver.PG_JDBC),
	COCKROACH("CockroachDB", JdbcDriver.PG_JDBC),
	;

	private final String displayName;
	final JdbcDriver driver;

	Vendor(String displayName, JdbcDriver driver) {
		this.displayName = displayName;
		this.driver = driver;
	}

	@Override
	public String toString() {
		return displayName;
	}

	public boolean isRemote() {
		return isMySQLLike() || isPostgresLike();
	}

	public boolean isMySQLLike() {
		return driver == JdbcDriver.MARIADB_CONNECTOR;
	}

	boolean isPostgresLike() {
		return driver == JdbcDriver.PG_JDBC;
	}

	public SQLDialect dialect() {
		return switch (this) {
			case HSQLDB -> SQLDialect.HSQLDB;
			case MARIADB -> SQLDialect.MARIADB;
			case MYSQL -> SQLDialect.MYSQL;
			case POSTGRES, COCKROACH -> SQLDialect.POSTGRES;
		};
	}

	/**
	 * Minimum version needed for healthy operability of LibertyBans
	 *
	 * @return the minimum RDMS version for efficient, healthy operation
	 */
	Optional<String> requiredMinimumVersion() {
		return switch (this) {
			case HSQLDB, COCKROACH -> // Assume CockroachDB users know what they are doing
					Optional.empty();
			case MARIADB ->
				// Justification
				// 10.6 adds standards-compliant OFFSET/LIMIT, needed by JOOQ https://mariadb.com/kb/en/select-offset-fetch/
				// 10.3 adds sequences https://mariadb.com/kb/en/create-sequence/
				// 10.3 adds SIMULTANEOUS_ASSIGNMENT https://jira.mariadb.org/browse/MDEV-13417
				// 10.2 is unsupported by Flyway
				Optional.of("10.6");
			case MYSQL ->
				// Justification
				// 8.0 enforces CHECK constraints https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html
				// 5.7 is unsupported by Flyway
					Optional.of("8.0");
			case POSTGRES ->
				// Justification
				// 12 adds generated columns https://www.postgresql.org/docs/12/ddl-generated-columns.html
					Optional.of("12");
		};
	}

	/**
	 * The lowest supported retrograde version allowed if {@link #requiredMinimumVersion()} is not met
	 *
	 * @return the lowest supported version whose use may entail compatibility hacks
	 */
	Optional<String> retroSupportVersion() {
		return (this == MARIADB) ? Optional.of("10.3") : Optional.empty();
	}

	public String getGeneratedColumnSuffix() {
		return switch (this) {
			case HSQLDB -> "";
			case MARIADB, MYSQL, COCKROACH -> " VIRTUAL";
			case POSTGRES -> " STORED";
		};
	}

	public String getExtraTableOptions() {
		return switch (this) {
			case HSQLDB, POSTGRES, COCKROACH -> "";
			case MARIADB, MYSQL -> " CHARACTER SET utf8mb4 COLLATE utf8mb4_bin";
		};
	}

	public String uuidType() {
		return switch (this) {
			case HSQLDB, POSTGRES, COCKROACH -> "UUID";
			case MARIADB, MYSQL -> "BINARY(16)";
		};
	}

	public String inetType() {
		if (isPostgresLike()) {
			return "BYTEA";
		}
		return "VARBINARY(16)";
	}

	public String arbitraryBinaryType() {
		if (isPostgresLike()) {
			return "BYTEA";
		}
		return "BLOB";
	}

	public String alterViewStatement() {
		return switch (this) {
			case HSQLDB, MARIADB, MYSQL -> "ALTER VIEW";
			case POSTGRES, COCKROACH -> "CREATE OR REPLACE VIEW";
		};
	}

	public String zeroSmallintLiteral() {
		return switch (this) {
			case HSQLDB, POSTGRES, COCKROACH -> "CAST(0 AS SMALLINT)";
			case MARIADB, MYSQL -> "0";
		};
	}

	public String[] migrateScope() {
		return switch (this) {
			case MARIADB, MYSQL -> new String[] {"", ""};
			case HSQLDB, POSTGRES, COCKROACH -> new String[] {
					"CAST(",
					" AS CHARACTER VARYING(32))"
			};
		};
	}

	String getConnectionInitSql() {

		return switch (this) {
			case HSQLDB -> "SET DATABASE TRANSACTION CONTROL MVLOCKS";
			case MARIADB -> "SET NAMES utf8mb4 COLLATE utf8mb4_bin; " + setSqlModes(
					// MariaDB defaults
					// Specify explicitly so that unwise shared hosts do not cause issues
					"STRICT_TRANS_TABLES",
					"ERROR_FOR_DIVISION_BY_ZERO",
					"NO_AUTO_CREATE_USER",
					"NO_ENGINE_SUBSTITUTION",
					// Modes specifically used by LibertyBans, for better ANSI SQL compliance
					"ANSI",
					"NO_BACKSLASH_ESCAPES",
					"SIMULTANEOUS_ASSIGNMENT", // MDEV-13417
					"NO_ZERO_IN_DATE",
					"NO_ZERO_DATE");
			case MYSQL -> "SET NAMES utf8mb4 COLLATE utf8mb4_bin; " + setSqlModes(
					// MySQL defaults, set explicitly
					"STRICT_TRANS_TABLES",
					"ERROR_FOR_DIVISION_BY_ZERO",
					"NO_ENGINE_SUBSTITUTION",
					// Modes specifically used by LibertyBans
					"ANSI",
					"NO_BACKSLASH_ESCAPES",
					"NO_ZERO_IN_DATE",
					"NO_ZERO_DATE");
			case POSTGRES -> "SET NAMES 'UTF8'";
			case COCKROACH -> {
				Map<String, Object> sessionVariables = Map.of(
						// SQL standard compliance
						"default_int_size", 4,
						"sql_safe_updates", "off",
						// LibertyBans uses joins prodigiously
						"reorder_joins_limit", 6
				);
				yield sessionVariables
						.entrySet()
						.stream()
						.map((entry) -> "SET SESSION " + entry.getKey() + " = " + entry.getValue() + "; ")
						.collect(Collectors.joining());
			}
		};
	}

	private static String setSqlModes(String...sqlModes) {
		String modes = String.join(",", sqlModes);
		return "SET @@SQL_MODE = CONCAT(@@SQL_MODE, '," + modes + "')";
	}

	public String userForITs() {
		return switch (this) {
			case HSQLDB -> "SA";
			case MARIADB, MYSQL, COCKROACH -> "root";
			case POSTGRES -> "postgres";
		};
	}

	public String passwordForITs() {
		return (this == POSTGRES) ? "pgpass" : "";
	}

}
