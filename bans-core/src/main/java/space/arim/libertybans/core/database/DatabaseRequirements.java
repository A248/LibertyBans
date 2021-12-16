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

import org.flywaydb.core.api.MigrationVersion;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.bootstrap.StartupException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public final class DatabaseRequirements {

	private final Vendor vendor;
	private final Connection connection;

	public DatabaseRequirements(Vendor vendor, Connection connection) {
		this.vendor = Objects.requireNonNull(vendor, "vendor");
		this.connection = Objects.requireNonNull(connection, "connection");
	}

	// We use the same MigrationVersion which flyway relies on
	private MigrationVersion databaseSemanticVersion() throws SQLException {
		DatabaseMetaData metaData = connection.getMetaData();
		return MigrationVersion.fromVersion(
				metaData.getDatabaseMajorVersion() + "." + metaData.getDatabaseMinorVersion()
		);
	}

	private boolean isMariaDbPerMetadata() throws SQLException {
		DatabaseMetaData metaData = connection.getMetaData();
		String databaseProductVersion = metaData.getDatabaseProductVersion();
		return databaseProductVersion.toLowerCase(Locale.ROOT).contains("mariadb");
	}

	public void checkRequirements() throws SQLException {
		if (vendor == Vendor.MARIADB && !isMariaDbPerMetadata()) {
			throw specifyThisVendorInstead(Vendor.MYSQL);
		}
		if (vendor == Vendor.MYSQL && isMariaDbPerMetadata()) {
			throw specifyThisVendorInstead(Vendor.MARIADB);
		}
		Optional<String> minimumVersion = vendor.requiredMinimumVersion();
		if (minimumVersion.isPresent()) {
			requireMinimumVersion(minimumVersion.get());
		}
		checkGrants();
	}

	private StartupException specifyThisVendorInstead(Vendor actual) {
		return databaseMisconfiguration("You specified that you are using " + vendor + " (in sql.yml). " +
				"However, we detected that you are really using " + actual + ". " +
				"Please set the option to '" + actual.name() + "' since you are really using " + actual);
	}

	private void requireMinimumVersion(String requiredVersion) throws SQLException {
		if (!databaseSemanticVersion().isAtLeast(requiredVersion)) {
			throw databaseMisconfiguration("If you are using " + vendor + ", you must be on " +
					vendor + " " + requiredVersion + " or a newer version. " +
					"Earlier versions are not supported. You are responsible for keeping your database up-to-date.");
		}
	}

	private void checkGrants() throws SQLException {
		// Pterodactyl has a years-old bug where it does not grant proper SQL permissions
		// https://github.com/pterodactyl/panel/issues/3761
		// Check grants manually to provide a user-friendly error message and reduce the support burden
		if (vendor.isMySQLLike()) {
			String actualGrants;
			try (Statement statement = connection.createStatement();
				 ResultSet grantResultSet = statement.executeQuery("SHOW GRANTS")) {

				StringJoiner actualGrantsBuilder = new StringJoiner("\n");
				while (grantResultSet.next()) {
					actualGrantsBuilder.add(grantResultSet.getString(1));
				}
				actualGrants = actualGrantsBuilder.toString();
			}
			/*
			Examples of what actualGrants might look like:
			- GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, CREATE ROLE, DROP ROLE ON *.* TO "root"@"%"
			- GRANT ALL PRIVILEGES ON *.* TO "root"@"%" WITH GRANT OPTION
			 */
			if (actualGrants.contains("ALL PRIVILEGES")) {
				return;
			}
			List<String> requiredGrants = List.of(
					"ALTER", "ALTER ROUTINE", "CREATE", "CREATE ROUTINE", "CREATE TEMPORARY TABLES", "CREATE VIEW",
					"DELETE", "DROP", "EVENT", "EXECUTE", "INDEX", "INSERT", "LOCK TABLES", "REFERENCES",
					"SELECT", "SHOW VIEW", "TRIGGER", "UPDATE");
			for (String requiredGrant : requiredGrants) {
				if (actualGrants.contains(requiredGrant + ",") // not the last privilege listed
						|| actualGrants.contains(", " + requiredGrant + " ON") /* last privilege listed */) {
					continue;
				}
				LoggerFactory.getLogger(getClass()).debug("Full set of privileges detected: {}", actualGrants);
				throw databaseMisconfiguration("The database user has insufficient permissions. " +
						"LibertyBans needs full permissions to function properly.\n" +
						"You MUST grant the proper set of privileges to the database user in order to use " + vendor + ". " +
						"Missing permission: " + requiredGrant +
						"\n\n" +
						"If you are using pterodatyl, this is a known bug which must be solved by pterodactyl.\n" +
						"We suggest either not using pterodactyl or, if you know how, modifying your database installation.");
			}
		}
	}

	private StartupException databaseMisconfiguration(String message) {
		return new StartupException("\n" +
				"---- Your database server (" + vendor + ") is misconfigured. ----" +
				"\n" +
				message +
				"\n\n" +
				"For additional support, please join the LibertyBans discord where we can help you with your database.");
	}
}
