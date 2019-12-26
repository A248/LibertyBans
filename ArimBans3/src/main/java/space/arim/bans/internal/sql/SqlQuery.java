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

import space.arim.bans.api.sql.ExecutableQuery;
import space.arim.bans.api.util.StringsUtil;

import space.arim.universal.util.collections.CollectionsUtil;

public class SqlQuery {
	
	private final Query statement;
	private final Object[] parameters;
	
	static final int SUBJECT_COLUMN_SIZE = 52;
	
	public SqlQuery(Query statement, Object...parameters) {
		this.statement = statement;
		this.parameters = parameters;
	}
	
	public ExecutableQuery convertToExecutable(SqlSettings settings) {
		return new ExecutableQuery(statement.eval(settings), parameters);
	}
	
	public Query statement() {
		return statement;
	}
	
	public Object[] parameters() {
		return parameters;
	}
	
	@Override
	public String toString() {
		return "{[" + statement + "] with parameters [" + StringsUtil.concat(CollectionsUtil.convertAll(parameters, (param) -> param.toString()), ',') + "]}";
	}
	
	public enum Query {
		CREATE_TABLE_ACTIVE(
				"CREATE TABLE IF NOT EXISTS `%PREFIX%active` ("
						+ "`id` int NOT NULL,"
						+ "`type` VARCHAR(3) NOT NULL,"
						+ "`subject` VARCHAR(" + SUBJECT_COLUMN_SIZE + ") NOT NULL,"
						+ "`operator` VARCHAR(" + SUBJECT_COLUMN_SIZE + ") NOT NULL,"
						+ "`reason` VARCHAR(255) NOT NULL,"
						+ "`expiration` BIGINT NOT NULL,"
						+ "`date` BIGINT NOT NULL)"),

		CREATE_TABLE_HISTORY(
				"CREATE TABLE IF NOT EXISTS `%PREFIX%history` ("
						+ "`id` int NOT NULL,"
						+ "`type` VARCHAR(3) NOT NULL,"
						+ "`subject` VARCHAR(" + SUBJECT_COLUMN_SIZE + ") NOT NULL,"
						+ "`operator` VARCHAR(" + SUBJECT_COLUMN_SIZE + ") NOT NULL,"
						+ "`reason` VARCHAR(255) NOT NULL,"
						+ "`expiration` BIGINT NOT NULL,"
						+ "`date` BIGINT NOT NULL)"),

		CREATE_TABLE_CACHE(
				"CREATE TABLE IF NOT EXISTS `%PREFIX%cache` ("
						+ "`uuid` VARCHAR(31) NOT NULL,"
						+ "`name` VARCHAR(15) NOT NULL,"
						+ "`iplist` TEXT NOT NULL,"
						+ "`update_name` BIGINT NOT NULL," 
						+ "`update_iplist` BIGINT NOT NULL)"),

		INSERT_ACTIVE(
				"INSERT INTO `%PREFIX%active` "
						+ "(`id`, `type`, `subject`, `operator`, `reason`, `expiration`, `date`) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?)"),

		INSERT_HISTORY(
				"INSERT INTO `%PREFIX%history` "
						+ "(`id`, `type`, `subject`, `operator`, `reason`, `expiration`, `date`) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?)"),

		INSERT_CACHE("INSERT INTO `%PREFIX%cache` " 
				+ "(`uuid`, `name`, `iplist`, `update_name`, `update_iplist`) " 
				+ "VALUES (?, ?, ?, ?, ?)"),

		DELETE_ACTIVE_BY_ID(
				"DELETE FROM `%PREFIX%active` WHERE `id` = ?"),
		
		REFRESH_ACTIVE(
				"DELETE FROM `%PREFIX%active` WHERE `expiration` <= ? AND `expiration` != -1"),
		
		UPDATE_ACTIVE_REASON_FOR_ID(
				"UPDATE `%PREFIX%active` SET `reason` = ? WHERE `id` = ?"),
		
		UPDATE_HISTORY_REASON_FOR_ID(
				"UPDATE `%PREFIX%history` SET `reason` = ? WHERE `id` = ?"),
		
		UPDATE_CACHE_FOR_UUID(
				"UPDATE `%PREFIX%cache` SET `name` = ?, `iplist` = ?, `update-name` = ?, `update-iplist` = ? WHERE `uuid` = ?"),
		
		UPDATE_NAME_FOR_UUID(
				"UPDATE `%PREFIX%cache` SET `name` = ?, `update-name` = ? WHERE `uuid` = ?"),
		
		UPDATE_IPS_FOR_UUID(
				"UPDATE `%PREFIX%cache` SET `iplist` = ?, `update-iplist` = ? WHERE `uuid` = ?"),
		
		SELECT_ALL_ACTIVE("SELECT * FROM `%PREFIX%active`"),
		
		SELECT_ALL_HISTORY("SELECT * FROM `%PREFIX%history`"),
		
		SELECT_ALL_CACHED("SELECT * FROM `%PREFIX%cache`");

		private String mysql;

		private Query(String mysql) {
			this.mysql = mysql;
		}
		
		String eval(SqlSettings settings) {
			String statement = settings.toString().equals("mysql") ? mysql : mysql.replace(" int ", " INTEGER ").replace(" NOT NULL", "").replace("`", "").replace("TEXT", "CLOB");
			return statement.replace("%PREFIX%", settings.prefix);
		}
		
		@Override
		public String toString() {
			return mysql;
		}
		
	}
}
