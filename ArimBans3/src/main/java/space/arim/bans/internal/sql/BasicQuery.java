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

public class BasicQuery implements Query {
	
	private final PreQuery preQuery;
	private final Object[] parameters;
	
	static final int SUBJECT_COLUMN_SIZE = 53;
	
	public BasicQuery(PreQuery preQuery, Object...parameters) {
		this.preQuery = preQuery;
		this.parameters = parameters;
	}
	
	@Override
	public String statement() {
		return preQuery.toString();
	}
	
	@Override
	public Object[] parameters() {
		return parameters;
	}
	
	public enum PreQuery {
		CREATE_TABLE_ACTIVE(
				"CREATE TABLE IF NOT EXISTS `%PREFIX%active` ("
						+ "`id` int NOT NULL,"
						+ "`type` VARCHAR(4) NOT NULL,"
						+ "`subject` VARCHAR(" + SUBJECT_COLUMN_SIZE + ") NOT NULL,"
						+ "`operator` VARCHAR(" + SUBJECT_COLUMN_SIZE + ") NOT NULL,"
						+ "`reason` TEXT NOT NULL,"
						+ "`expiration` BIGINT NOT NULL,"
						+ "`date` BIGINT NOT NULL)"),

		CREATE_TABLE_HISTORY(
				"CREATE TABLE IF NOT EXISTS `%PREFIX%history` ("
						+ "`id` int NOT NULL,"
						+ "`type` VARCHAR(4) NOT NULL,"
						+ "`subject` VARCHAR(" + SUBJECT_COLUMN_SIZE + ") NOT NULL,"
						+ "`operator` VARCHAR(" + SUBJECT_COLUMN_SIZE + ") NOT NULL,"
						+ "`reason` TEXT NOT NULL,"
						+ "`expiration` BIGINT NOT NULL,"
						+ "`date` BIGINT NOT NULL)"),

		CREATE_TABLE_CACHE(
				"CREATE TABLE IF NOT EXISTS `%PREFIX%cache` ("
						+ "`uuid` VARCHAR(32) NOT NULL,"
						+ "`name` VARCHAR(16) NOT NULL,"
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
				"UPDATE `%PREFIX%cache` SET `iplist` = ?, `update-iplist` = ? WHERE `uuid` = ?");

		private String statement;

		private PreQuery(String statement) {
			this.statement = statement;
		}
		
		@Override
		public String toString() {
			return statement;
		}
	}
}
