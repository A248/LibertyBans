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

package space.arim.libertybans.core.importing;

import space.arim.omnibus.util.UUIDUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Responsible for determing the UUID BanManager uses as the UUID of the console
 *
 */
class BanManagerConsoleUUID {

	private final ConnectionSource connectionSource;
	private final String tablePrefix;

	BanManagerConsoleUUID(ConnectionSource connectionSource, String tablePrefix) {
		this.connectionSource = connectionSource;
		this.tablePrefix = tablePrefix;
	}

	UUID retrieveConsoleUUID() {
		try (Connection connection = connectionSource.openConnection();
			 PreparedStatement preparedStatement = connection.prepareStatement(
					 "SELECT \"id\" FROM \"" + tablePrefix + "players\" ORDER BY \"lastSeen\" ASC LIMIT 1");
			 ResultSet resultSet = preparedStatement.executeQuery()) {

			if (!resultSet.next()) {
				throw new ImportException("ResultSet.next() returned false while trying to find console UUID");
			}
			return UUIDUtil.fromByteArray(resultSet.getBytes("id"));

		} catch (SQLException ex) {
			throw new ImportException("Unable to retrieve console UUID", ex);
		}
	}
}
