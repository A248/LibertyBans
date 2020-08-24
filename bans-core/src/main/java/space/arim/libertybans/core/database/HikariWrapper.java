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
package space.arim.libertybans.core.database;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariDataSource;

import space.arim.jdbcaesar.ConnectionSource;

class HikariWrapper implements ConnectionSource {

	private final HikariDataSource hikariDataSource;
	
	HikariWrapper(HikariDataSource hikariDataSource) {
		this.hikariDataSource = hikariDataSource;
	}
	
	HikariDataSource getHikariDataSource() {
		return hikariDataSource;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return hikariDataSource.getConnection();
	}

	@Override
	public void close() {
		hikariDataSource.close();
	}
	
}
