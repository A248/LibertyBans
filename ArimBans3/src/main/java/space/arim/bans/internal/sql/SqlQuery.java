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
	
	private final Query statement;
	private final Object[] parameters;
	
	public SqlQuery(Query statement, Object...params) {
		this.statement = statement;
		this.parameters = params;
	}
	
	public Query statement() {
		return this.statement;
	}
	
	public Object[] parameters() {
		return this.parameters;
	}
	
	public enum Query {
		CREATE_TABLE_ACTIVE(
				"CREATE TABLE IF NOT EXISTS `%PREFIX%active` (" + "`id` int NOT NULL AUTO_INCREMENT,"
						+ "`type` VARCHAR(31) NOT NULL," + "`subject` VARCHAR(45) NOT NULL,"
						+ "`operator` VARCHAR(45) NOT NULL," + "`reason` VARCHAR(255) NOT NULL,"
						+ "`expiration` BIGINT NOT NULL," + "`date` BIGINT NOT NULL," + "PRIMARY KEY (`id`))",

				"CREATE TABLE IF NOT EXISTS %PREFIX%active (" + "id INTEGER IDENTITY PRIMARY KEY," + "type VARCHAR(31),"
						+ "subject VARCHAR(49)," + "operator VARCHAR(49)," + "reason VARCHAR(255)," + "expiration BIGINT,"
						+ "date BIGINT)"),

		CREATE_TABLE_HISTORY(
				"CREATE TABLE IF NOT EXISTS `%PREFIX%history` (" + "`id` int NOT NULL AUTO_INCREMENT,"
						+ "`type` VARCHAR(31) NOT NULL," + "`subject` VARCHAR(45) NOT NULL,"
						+ "`operator` VARCHAR(45) NOT NULL," + "`reason` VARCHAR(255) NOT NULL,"
						+ "`expiration` BIGINT NOT NULL," + "`date` BIGINT NOT NULL," + "PRIMARY KEY (`id`))",

				"CREATE TABLE IF NOT EXISTS %PREFIX%history (" + "id INTEGER IDENTITY PRIMARY KEY," + "type VARCHAR(31),"
						+ "subject VARCHAR(49)," + "operator VARCHAR(49)," + "reason VARCHAR(255)," + "expiration BIGINT,"
						+ "date BIGINT)"),

		CREATE_TABLE_CACHE(
				"CREATE TABLE IF NOT EXISTS `%PREFIX%cache` (" 
						+ "`index` int NOT NULL AUTO_INCREMENT,"
						+ "`uuid` VARCHAR(31) NOT NULL," 
						+ "`name` VARCHAR(15) NOT NULL,"
						+ "`iplist` TEXT NOT NULL," 
						+ "`update_name` BIGINT NOT NULL," 
						+ "`update_iplist` BIGINT NOT NULL)",
				"CREATE TABLE IF NOT EXISTS %PREFIX%cache (" 
						+ "index INTEGER IDENTITY PRIMARY KEY," 
						+ "uuid VARCHAR(31),"
						+ "name VARCHAR(15)," 
						+ "iplist TEXT," 
						+ "update_name BIGINT,"
						+ "update_iplist BIGINT)"),

		INSERT_ACTIVE(
				"INSERT INTO `%PREFIX%active` " + "(`type`, `subject`, `operator`, `reason`, `expiration`, `date`) "
						+ "VALUES (?, ?, ?, ?, ?)",

				"INSERT INTO %PREFIX%active " + "(type, subject, operator, reason, expiration, date) " + "VALUES (?, ?, ?, ?, ?)"),

		INSERT_HISTORY(
				"INSERT INTO `%PREFIX%history` " + "(`type`, `subject`, `operator`, `reason`, `expiration`, `date`) "
						+ "VALUES (?, ?, ?, ?, ?)",

				"INSERT INTO %PREFIX%history " + "(type, subject, operator, reason, expiration, date) " + "VALUES (?, ?, ?, ?, ?)"),

		INSERT_CACHE("INSERT INTO `%PREFIX%cache` " 
				+ "(`uuid`, `name`, `iplist`, `update_name`, `update_iplist`) " 
				+ "VALUES (?, ?, ?, ?, ?)",

				"INSERT INTO %PREFIX%cache " 
				+ "(uuid, name, iplist, update_name, update_iplist) " 
				+ "VALUES (?, ?, ?, ?, ?)"),

		DELETE_ACTIVE_FROM_DATE(
				"DELETE FROM `%PREFIX%active` WHERE `date` = ?", 
				"DELETE FROM %PREFIX%active WHERE date = ?"),
		REFRESH_ACTIVE("DELETE FROM `%PREFIX%active` WHERE `expiration` <= ? AND `expiration` != -1",
				"DELETE FROM %PREFIX%active WHERE expiration <= ? AND expiration != -1"),
		
		UPDATE_ACTIVE_REASON_FROM_DATE("UPDATE `%PREFIX%active` SET `reason` = ? WHERE `date` = ?",
				"UPDATE %PREFIX%active SET reason = ? WHERE date = ?"),
		UPDATE_HISTORY_REASON_FROM_DATE("UPDATE `%PREFIX%history` SET `reason` = ? WHERE `date` = ?",
				"UPDATE %PREFIX%history SET reason = ? WHERE date = ?"),
		
		UPDATE_IPS_FOR_UUID(
				"UPDATE `%PREFIX%cache` SET `iplist` = ?, `update-iplist` = ? WHERE `uuid` = ?",
				"UPDATE %PREFIX%cache SET iplist = ?, update-iplist = ? WHERE uuid = ?"),
		
		UPDATE_NAME_FOR_UUID(
				"UPDATE `%PREFIX%cache` SET `name` = ?, `update-name` = ? WHERE `uuid` = ?",
				"UPDATE %PREFIX%cache SET name = ?, update-name = ? WHERE uuid = ?"),
		
		SELECT_ALL_ACTIVE("SELECT * FROM `%PREFIX%active`", "SELECT * FROM %PREFIX%active"),
		SELECT_ALL_HISTORY("SELECT * FROM `%PREFIX%history`", "SELECT * FROM %PREFIX%history"),
		SELECT_ALL_CACHED("SELECT * FROM `%PREFIX%cache`", "SELECT * FROM %PREFIX%cache");

		private String mysql;
		private String file;

		private Query(String mysql, String file) {
			this.mysql = mysql;
			this.file = file;
		}
		
		String eval(SqlSettings settings) {
			String statement = (settings.getStorageModeName().equals("mysql")) ? mysql : file;
			return statement.replace("%PREFIX%", settings.prefix);
		}
	}
}
