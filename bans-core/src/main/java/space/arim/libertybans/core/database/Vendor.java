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

package space.arim.libertybans.core.database;

import org.jooq.SQLDialect;
import space.arim.libertybans.core.punish.MiscUtil;

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
	private final JdbcDriver driver;

	private Vendor(String displayName, JdbcDriver driver) {
		this.displayName = displayName;
		this.driver = driver;
	}

	@Override
	public String toString() {
		return displayName;
	}

	JdbcDriver driver() {
		return driver;
	}

	public boolean hasDeleteFromJoin() {
		return isMySQLLike();
	}

	boolean isRemote() {
		return isMySQLLike() || isPostgresLike();
	}

	public boolean isMySQLLike() {
		return driver == JdbcDriver.MARIADB_CONNECTOR;
	}

	boolean isPostgresLike() {
		return driver == JdbcDriver.PG_JDBC;
	}

	public SQLDialect dialect() {
		switch (this) {
		case HSQLDB:
			return SQLDialect.HSQLDB;
		case MARIADB:
			return SQLDialect.MARIADB;
		case MYSQL:
			return SQLDialect.MYSQL;
		case POSTGRES:
		case COCKROACH:
			return SQLDialect.POSTGRES;
		default:
			throw MiscUtil.unknownVendor(this);
		}
	}

	Optional<String> requiredMinimumVersion() {
		switch (this) {
		case HSQLDB:
		case COCKROACH: // Assume CockroachDB users know what they are doing
			return Optional.empty();
		case MARIADB:
			// Justification
			// 10.6 adds standards-compliant OFFSET/LIMIT, needed by JOOQ https://mariadb.com/kb/en/select-offset-fetch/
			// 10.3 adds sequences https://mariadb.com/kb/en/create-sequence/
			// 10.3 adds SIMULTANEOUS_ASSIGNMENT https://jira.mariadb.org/browse/MDEV-13417
			// 10.2 is unsupported by Flyway
			return Optional.of("10.6");
		case MYSQL:
			// Justification
			// 8.0 enforces CHECK constraints https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html
			// 5.7 is unsupported by Flyway
			return Optional.of("8.0");
		case POSTGRES:
			// Justification
			// 12 adds generated columns https://www.postgresql.org/docs/12/ddl-generated-columns.html
			return Optional.of("12");
		default:
			throw MiscUtil.unknownVendor(this);
		}
	}

	public String getGeneratedColumnSuffix() {
		switch (this) {
		case HSQLDB:
			return "";
		case MARIADB:
		case MYSQL:
		case COCKROACH:
			return " VIRTUAL";
		case POSTGRES:
			return " STORED";
		default:
			throw MiscUtil.unknownVendor(this);
		}
	}

	public String getExtraTableOptions() {
		switch (this) {
		case HSQLDB:
		case POSTGRES:
		case COCKROACH:
			return "";
		case MARIADB:
		case MYSQL:
			return " CHARACTER SET utf8mb4 COLLATE utf8mb4_bin";
		default:
			throw MiscUtil.unknownVendor(this);
		}
	}

	public String uuidType() {
		switch (this) {
		case HSQLDB:
		case POSTGRES:
		case COCKROACH:
			return "UUID";
		case MARIADB:
		case MYSQL:
			return "BINARY(16)";
		default:
			throw MiscUtil.unknownVendor(this);
		}
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

	String getConnectionInitSql() {
		switch (this) {
		case HSQLDB:
			return "SET DATABASE TRANSACTION CONTROL MVLOCKS";
		case MARIADB:
			return "SET NAMES utf8mb4 COLLATE utf8mb4_bin; " + setSqlModes(
					// MariaDB defaults
					// Specify explicitly so that unwise shared hosts do not cause issues
					"STRICT_TRANS_TABLES",
					"ERROR_FOR_DIVISION_BY_ZERO",
					"NO_AUTO_CREATE_USER",
					"NO_ENGINE_SUBSTITUTION",
					// Modes specifically used by LibertyBans, for better ANSI SQL compliance
					"ANSI",
					"SIMULTANEOUS_ASSIGNMENT", // MDEV-13417
					"NO_ZERO_IN_DATE",
					"NO_ZERO_DATE");
		case MYSQL:
			return "SET NAMES utf8mb4 COLLATE utf8mb4_bin; " + setSqlModes(
					// MySQL defaults, set explicitly
					"STRICT_TRANS_TABLES",
					"ERROR_FOR_DIVISION_BY_ZERO",
					"NO_ENGINE_SUBSTITUTION",
					// Modes specifically used by LibertyBans
					"ANSI",
					"NO_ZERO_IN_DATE",
					"NO_ZERO_DATE");
		case POSTGRES:
			return "SET NAMES 'UTF8'";
		case COCKROACH:
			Map<String, Object> sessionVariables = Map.of(
					// SQL standard compliance
					"default_int_size", 4,
					"sql_safe_updates", "off",
					// LibertyBans uses joins prodigiously
					"reorder_joins_limit", 6
			);
			return sessionVariables
					.entrySet()
					.stream()
					.map((entry) -> "SET SESSION " + entry.getKey() + " = " + entry.getValue() + "; ")
					.collect(Collectors.joining());
		default:
			throw MiscUtil.unknownVendor(this);
		}
	}

	private static String setSqlModes(String...sqlModes) {
		String modes = String.join(",", sqlModes);
		return "SET @@SQL_MODE = CONCAT(@@SQL_MODE, '," + modes + "')";
	}

	public String userForITs() {
		switch (this) {
		case HSQLDB:
			return "SA";
		case MARIADB:
		case MYSQL:
		case COCKROACH:
			return "root";
		case POSTGRES:
			return "postgres";
		default:
			throw MiscUtil.unknownVendor(this);
		}
	}

	public String passwordForITs() {
		return (this == POSTGRES) ? "pgpass" : "";
	}
}
