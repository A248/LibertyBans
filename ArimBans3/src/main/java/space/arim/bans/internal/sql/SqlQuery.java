/*
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.internal.sql;

public class SqlQuery {
	
	private final String statement;
	private final Object[] parameters;
	
	public SqlQuery(String statement, Object...params) {
		this.statement = statement;
		this.parameters = params;
	}
	
	public String statement() {
		return this.statement;
	}
	
	public Object[] parameters() {
		return this.parameters;
	}
	
	public enum Query {
		CREATE_TABLE_ACTIVE(
				"CREATE TABLE IF NOT EXISTS `%PREFIX%Active` (" + "`id` int NOT NULL AUTO_INCREMENT,"
						+ "`type` VARCHAR(31) NOT NULL," + "`subject` VARCHAR(45) NOT NULL,"
						+ "`operator` VARCHAR(45) NOT NULL," + "`reason` VARCHAR(255) NOT NULL,"
						+ "`expiration` BIGINT NOT NULL," + "`date` BIGINT NOT NULL," + "PRIMARY KEY (`id`))",

				"CREATE TABLE IF NOT EXISTS %PREFIX%Active (" + "id INTEGER IDENTITY PRIMARY KEY," + "type VARCHAR(31),"
						+ "subject VARCHAR(49)," + "operator VARCHAR(49)," + "reason VARCHAR(255)," + "expiration BIGINT,"
						+ "date BIGINT)"),

		CREATE_TABLE_HISTORY(
				"CREATE TABLE IF NOT EXISTS `%PREFIX%History` (" + "`id` int NOT NULL AUTO_INCREMENT,"
						+ "`type` VARCHAR(31) NOT NULL," + "`subject` VARCHAR(45) NOT NULL,"
						+ "`operator` VARCHAR(45) NOT NULL," + "`reason` VARCHAR(255) NOT NULL,"
						+ "`expiration` BIGINT NOT NULL," + "`date` BIGINT NOT NULL," + "PRIMARY KEY (`id`))",

				"CREATE TABLE IF NOT EXISTS %PREFIX%History (" + "id INTEGER IDENTITY PRIMARY KEY," + "type VARCHAR(31),"
						+ "subject VARCHAR(49)," + "operator VARCHAR(49)," + "reason VARCHAR(255)," + "expiration BIGINT,"
						+ "date BIGINT)"),

		CREATE_TABLE_CACHE(
				"CREATE TABLE IF NOT EXISTS `%PREFIX%Cache` (" 
						+ "`index` int NOT NULL AUTO_INCREMENT,"
						+ "`uuid` VARCHAR(31) NOT NULL," 
						+ "`name` VARCHAR(15) NOT NULL,"
						+ "`iplist` TEXT NOT NULL," 
						+ "`update_name` BIGINT NOT NULL," 
						+ "`update_iplist` BIGINT NOT NULL)",
				"CREATE TABLE IF NOT EXISTS %PREFIX%Cache (" 
						+ "index INTEGER IDENTITY PRIMARY KEY," 
						+ "uuid VARCHAR(31),"
						+ "name VARCHAR(15)," 
						+ "iplist TEXT," 
						+ "update_name BIGINT,"
						+ "update_iplist BIGINT)"),

		INSERT_ACTIVE(
				"INSERT INTO `%PREFIX%Active` " + "(`type`, `subject`, `operator`, `reason`, `expiration`, `date`) "
						+ "VALUES (?, ?, ?, ?, ?)",

				"INSERT INTO %PREFIX%Active " + "(type, subject, operator, reason, expiration, date) " + "VALUES (?, ?, ?, ?, ?)"),

		INSERT_HISTORY(
				"INSERT INTO `%PREFIX%History` " + "(`type`, `subject`, `operator`, `reason`, `expiration`, `date`) "
						+ "VALUES (?, ?, ?, ?, ?)",

				"INSERT INTO %PREFIX%History " + "(type, subject, operator, reason, expiration, date) " + "VALUES (?, ?, ?, ?, ?)"),

		INSERT_CACHE("INSERT INTO `%PREFIX%Cache` " 
				+ "(`uuid`, `name`, `iplist`, `update_name`, `update_iplist`) " 
				+ "VALUES (?, ?, ?, ?, ?)",

				"INSERT INTO %PREFIX%Cache " 
				+ "(uuid, name, iplist, update_name, update_iplist) " 
				+ "VALUES (?, ?, ?, ?, ?)"),

		DELETE_ACTIVE_FROM_DATE(
				"DELETE FROM `%PREFIX%Active` WHERE `date` = ?", 
				"DELETE FROM %PREFIX%Active WHERE date = ?"),
		REFRESH_ACTIVE("DELETE FROM `%PREFIX%Active` WHERE `expiration` <= ? AND `expiration` != -1",
				"DELETE FROM %PREFIX%Active WHERE expiration <= ? AND expiration != -1"),
		
		UPDATE_ACTIVE_REASON_FROM_DATE("UPDATE `%PREFIX%Active` SET `reason` = ? WHERE `date` = ?",
				"UPDATE %PREFIX%Active SET reason = ? WHERE date = ?"),
		UPDATE_HISTORY_REASON_FROM_DATE("UPDATE `%PREFIX%History` SET `reason` = ? WHERE `date` = ?",
				"UPDATE %PREFIX%History SET reason = ? WHERE date = ?"),
		
		UPDATE_IPS_FOR_UUID(
				"UPDATE `%PREFIX%Cache` SET `iplist` = ?, `update-iplist` = ? WHERE `uuid` = ?",
				"UPDATE %PREFIX%Cache SET iplist = ?, update-iplist = ? WHERE uuid = ?"),
		
		UPDATE_NAME_FOR_UUID(
				"UPDATE `%PREFIX%Cache` SET `name` = ?, `update-name` = ? WHERE `uuid` = ?",
				"UPDATE %PREFIX%Cache SET name = ?, update-name = ? WHERE uuid = ?"),
		
		SELECT_ALL_ACTIVE("SELECT * FROM `%PREFIX%Active`", "SELECT * FROM Active"),
		SELECT_ALL_HISTORY("SELECT * FROM `%PREFIX%History`", "SELECT * FROM History"),
		SELECT_ALL_CACHED("SELECT * FROM `%PREFIX%Cache`", "SELECT * FROM Cache");

		private String mysql;
		private String hsqldb;

		private Query(String mysql, String hsqldb) {
			this.mysql = mysql;
			this.hsqldb = hsqldb;
		}
		
		public String eval(SqlSettings settings) {
			String statement = (settings.mode.equals(StorageMode.HSQLDB)) ? hsqldb : mysql;
			return statement.replace("%PREFIX%", settings.prefix);
		}
	}
}
