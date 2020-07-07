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
package space.arim.libertybans.api;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;

/**
 * Access to the raw database. <b>Use of this should be avoided at great cost. Executing SQL queries
 * is not considered API. </b> <br>
 * <br>
 * The majority of users should never touch this directly. Instead, {@link PunishmentSelector} provides
 * predicate, error-free specifications using stable API. {@code PunishmentSelector} may be used to retrieve
 * punishments with all sorts of details from the database.
 * 
 * @author A248
 *
 */
public interface PunishmentDatabase {

	/**
	 * Gets a connection from the LibertyBans connection pool. <b>Use of this should be avoided at great cost.
	 * Executing SQL queries is not considered API. </b> <br>
	 * <br>
	 * The only API guarantee with respect to executing API queries is that the table {@code libertybans_revision}
	 * will contain a single row whose {@code revision} column indicates the data format LibertyBans maintains.
	 * It is guaranteed that whenever the table definitions change, this {@code revision} will be incremented.
	 * 
	 * @return a connection from the plugin pool
	 * @throws SQLException as relayed from JDBC
	 */
	Connection getConnection() throws SQLException;
	
	/**
	 * Gets the executor used by LibertyBans to execute queries. Typically, this executor
	 * has the same amount of threads as there are connections in the pool, making it
	 * a fine complement to executing queries.
	 * 
	 * @return the executor
	 */
	Executor getExecutor();
	
}
