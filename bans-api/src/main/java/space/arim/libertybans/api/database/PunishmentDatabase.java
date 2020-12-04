/* 
 * LibertyBans-api
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-api. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.api.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;

import space.arim.libertybans.api.select.PunishmentSelector;

/**
 * Access to the raw database. <b>Use of this should be avoided at great cost.
 * Executing SQL queries is not considered traditional API.</b> <br>
 * <br>
 * The majority of users should never touch this directly. Instead,
 * {@link PunishmentSelector} provides predictable, error-free functions
 * using stable API. {@code PunishmentSelector} may be used to retrieve
 * punishments with all sorts of details from the database. <br>
 * <br>
 * <b>API Status</b> <br>
 * Use of the SQL backend directly is not considered traditional API. Although
 * attempts will be made to limit the number of breaking changes, LibertyBans's
 * major version will not necessarily change to match the SQL backend. Unlike
 * other plugins, LibertyBans makes use of a wide swath of SQL queries and
 * operations, so a rigid table structure is not feasible. <br>
 * <br>
 * Rather, a <i>separate version</i> is maintained specifically for the SQL
 * backend. This version is returned by {@link #getMajorRevision()} and
 * {@link #getMinorRevision()}. Additionally, it is located in the table
 * <code>libertybans_revision</code>, with the {@code major} and {@code minor}
 * columns indicating the major version and minor version, respectively (both
 * columns use the INT data type). <br>
 * <br>
 * Whenever the tables and/or views definitions change incompatibly, this major
 * version will be incremented. This minor version will be incremented as new
 * tables and views are added or existing ones changed, but not in ways which
 * break backwards compatibility. <br>
 * <br>
 * Stored routines are not supported within any versioning context. Use of any
 * SQL functions or procedures created by LibertyBans is strictly unsupported.
 * 
 * @author A248
 *
 */
public interface PunishmentDatabase {

	/**
	 * Gets a connection from the LibertyBans connection pool. <br>
	 * <br>
	 * <b>Use of this should be avoided at great cost. Executing SQL queries is not
	 * considered traditional API.</b> See the class javadoc for more information.
	 * <br>
	 * <br>
	 * Furthermore, users of this method should not assume which vendor is used and
	 * thus avoid non standard SQL syntax.
	 * 
	 * @return a connection from the plugin pool, which should be closed immediately
	 *         after use
	 * @throws SQLException as relayed from JDBC
	 */
	Connection getConnection() throws SQLException;

	/**
	 * Gets the major revision of the SQL backend definitions, see class javadoc
	 * 
	 * @return the major revision
	 */
	int getMajorRevision();

	/**
	 * Gets the minor revision of the SQL backend definitions, see class javadoc
	 * 
	 * @return the minor revision
	 */
	int getMinorRevision();

	/**
	 * Gets the executor used by LibertyBans to execute queries. Typically, this
	 * executor has the same amount of threads as there are connections in the pool,
	 * making it a fine complement to executing queries.
	 * 
	 * @return the executor
	 */
	Executor getExecutor();

}
