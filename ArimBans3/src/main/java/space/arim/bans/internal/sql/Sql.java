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
import java.sql.SQLException;
import java.util.logging.Level;

import com.zaxxer.hikari.HikariDataSource;

import space.arim.bans.ArimBans;
import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.internal.sql.SqlQuery.Query;

import space.arim.api.sql.ExecutableQuery;
import space.arim.api.sql.PooledLoggingSql;

public class Sql extends PooledLoggingSql implements SqlMaster {

	private final ArimBans center;

	private static final String DEFAULTING_TO_STORAGE_MODE = "Invalid storage mode specified! Defaulting to FILE...";
	
	private HikariDataSource data;
	private SqlSettings settings;

	public Sql(ArimBans center) {
		this.center = center;
	}
	
	private void stopConnection() {
		if (!data.isClosed()) {
			data.close();
		}
	}
	
	@Override
	public String getStorageModeName() {
		return settings.toString();
	}
	
	@Override
	public boolean enabled() {
		return data != null && !data.isClosed();
	}
	
	@Override
	public void executeQuery(SqlQuery...queries) {
		try {
			executionQueries(convertAllToExecutable(queries));
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
	}
	
	@Override
	public void executeQuery(Query query, Object...params) {
		try {
			executionQuery(query.eval(settings), params);
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
	}
	
	@Override
	public ResultSet[] selectQuery(SqlQuery...queries) {
		try {
			return selectionQueries(convertAllToExecutable(queries));
		} catch (SQLException ex) {
			throw new InternalStateException("Query retrieval failed!", ex);
		}
	}
	
	@Override
	public ResultSet selectQuery(Query query, Object...params) {
		try {
			return selectionQuery(query.eval(settings), params);
		} catch (SQLException ex) {
			throw new InternalStateException("Query retrieval failed!", ex);
		}
	}
	
	private ExecutableQuery[] convertAllToExecutable(SqlQuery[] queries) {
		ExecutableQuery[] result = new ExecutableQuery[queries.length];
		for (int n = 0; n < queries.length; n++) {
			result[n] = queries[n].convertToExecutable(settings);
		}
		return result;
	}
	
	@Override
	protected void log(String message) {
		center.logs().log(Level.CONFIG, message);
	}
	
	@Override
	protected HikariDataSource getDataSource() {
		return data;
	}

	@Override
	public void close() {
		stopConnection();
	}
	
	private SqlSettings parseBackend(String input) {
		switch (input.toLowerCase()) {
		case "hsqldb":
		case "local":
		case "file":
		case "sqlite":
			return new LocalSettings(center.config());
		case "mysql":
		case "sql":
			return new RemoteSettings(center.config());
		default:
			center.logs().log(Level.WARNING, DEFAULTING_TO_STORAGE_MODE);
			return new LocalSettings(center.config());
		}
	}
	
	@Override
	public void refreshConfig(boolean first) {
		
		settings = parseBackend(center.config().getConfigString("storage.mode"));
		
		if (first || center.config().getConfigBoolean("storage.restart-on-reload")) {
			center.logs().log(Level.CONFIG, "Loading data backend " + settings);
			if (data != null) {
				data.close();
			}
			data = settings.loadDataSource();
		}
		
	}

}
