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

import org.jooq.DSLContext;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.database.PunishmentDatabase;
import space.arim.libertybans.core.database.execute.QueryExecutor;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

public interface InternalDatabase extends QueryExecutor {

	PunishmentDatabase asExternal();

	Vendor getVendor();

	void clearExpiredPunishments(DSLContext context, PunishmentType type, Instant currentTime);

	/**
	 * Designed to be used by testing, to clear all tables after one integration test
	 * 
	 */
	void truncateAllTables();

	/**
	 * Obtains a direct JDBC connection
	 *
	 * @return the direct JDBC connection
	 * @throws SQLException if the connection could not be acquired
	 */
	Connection getConnection() throws SQLException;
}
