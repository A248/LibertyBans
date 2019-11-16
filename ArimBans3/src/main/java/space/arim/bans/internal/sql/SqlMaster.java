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
