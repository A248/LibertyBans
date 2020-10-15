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

public enum Vendor {

	MARIADB("MariaDB", "org.mariadb.jdbc.Driver", "org.mariadb.jdbc.MariaDbDataSource"),
	HSQLDB("HyperSQL", "org.hsqldb.jdbc.JDBCDriver", "org.hsqldb.jdbc.JDBCDataSource");
	
	private final String displayName;
	private final String driverClassName;
	private final String dataSourceClassName;
	
	private Vendor(String displayName, String driverClassName, String dataSourceClassName) {
		this.displayName = displayName;
		this.driverClassName = driverClassName;
		this.dataSourceClassName = dataSourceClassName;
	}
	
	String displayName() {
		return displayName;
	}
	
	String driverClassName() {
		return driverClassName;
	}
	
	String dataSourceClassName() {
		return dataSourceClassName;
	}
	
	public boolean useStoredRoutines() {
		return this == MARIADB;
	}

	public boolean hasDeleteFromJoin() {
		return this == MARIADB;
	}
	
}
