/* 
 * LibertyBans-database
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-database is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-database is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with LibertyBans-database. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.libertybans.driver;

import org.hsqldb.jdbc.JDBCDataSource;
import org.mariadb.jdbc.MariaDbDataSource;

import com.zaxxer.hikari.HikariConfig;

public class DriverCreator {

	private final HikariConfig hikariConf;
	private final boolean jdbcUrl;
	
	public DriverCreator(HikariConfig hikariConf, boolean jdbcUrl) {
		this.hikariConf = hikariConf;
		this.jdbcUrl = jdbcUrl;
	}
	
	public void createMariaDb(String host, int port, String database) {
		if (jdbcUrl) {
			hikariConf.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database);
			setDriverClassName("org.mariadb.jdbc.Driver");
		} else {
			MariaDbDataSource mariaDbDs = new MariaDbDataSource(host, port, database);
			hikariConf.setDataSource(mariaDbDs);
		}
		/*
		hikariConf.addDataSourceProperty("cachePrepStmts", "true");
		hikariConf.addDataSourceProperty("prepStmtCacheSize", "25");
		hikariConf.addDataSourceProperty("prepStmtCacheSqlLimit", "256");
		hikariConf.addDataSourceProperty("useServerPrepStmts", "true");
		*/
	}
	
	public void createHsqldb(String file) {
		if (jdbcUrl) {
			hikariConf.setJdbcUrl("jdbc:hsqldb:file:" + file);
			setDriverClassName("org.hsqldb.jdbc.JDBCDriver");
		} else {
			JDBCDataSource hsqldbDs = new JDBCDataSource();
			hsqldbDs.setUrl("jdbc:hsqldb:file:" + file);
			hikariConf.setDataSource(hsqldbDs);
		}
	}
	
	private void setDriverClassName(String driverClassName) {
		Thread currentThread = Thread.currentThread();
		ClassLoader initialContextLoader = currentThread.getContextClassLoader();
		try {
			currentThread.setContextClassLoader(getClass().getClassLoader());
			hikariConf.setDriverClassName(driverClassName);
		} finally {
			currentThread.setContextClassLoader(initialContextLoader);
		}
	}
	
}
