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

import java.sql.ResultSet;

import space.arim.bans.api.exception.TypeParseException;
import space.arim.bans.internal.Replaceable;

public interface SqlMaster extends Replaceable {
	
	public StorageMode mode();
	
	public boolean enabled();
	
	public void executeQuery(SqlQuery...queries);
	
	public void executeQuery(String sql, Object...params);
	
	public ResultSet[] selectQuery(SqlQuery...queries);
	
	public ResultSet selectQuery(String sql, Object...params);
	
	public enum StorageMode {
		HSQLDB, MYSQL;
		public static StorageMode fromString(String mode) {
			switch (mode.toLowerCase()) {
			case "hsqldb":
				return StorageMode.HSQLDB;
			case "local":
				return StorageMode.HSQLDB;
			case "file":
				return StorageMode.HSQLDB;
			case "sqlite":
				return StorageMode.HSQLDB;
			case "mysql":
				return StorageMode.MYSQL;
			case "sql":
				return StorageMode.MYSQL;
			default:
				throw new TypeParseException(mode, StorageMode.class);
			}
		}
	}
}
