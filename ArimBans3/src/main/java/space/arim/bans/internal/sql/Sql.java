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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

import com.zaxxer.hikari.HikariDataSource;

import space.arim.bans.ArimBans;
import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.api.exception.TypeParseException;

public class Sql implements SqlMaster {

	private final ArimBans center;

	private static final String DEFAULTING_TO_STORAGE_MODE = "Invalid storage mode specified! Defaulting to FILE...";
	
	private HikariDataSource data;

	private SqlSettings settings;
	
	private RowSetFactory factory;

	public Sql(ArimBans center) {
		this.center = center;
		try {
			factory = RowSetProvider.newFactory();
		} catch (SQLException ex) {
			throw new InternalStateException("RowSetProvider could not load its factory!", ex);
		}
	}
	
	private void stopConnection() {
		if (!data.isClosed()) {
			data.close();
		}
	}
	
	private void replaceParams(PreparedStatement statement, Object...parameters) throws SQLException {
		for (int n = 0; n < parameters.length; n++) {
			statement.setObject(n, parameters[n]);
		}
	}
	
	@Override
	public SqlSettings settings() {
		return settings;
	}
	
	@Override
	public boolean enabled() {
		return (data != null) ? !data.isClosed() : false;
	}

	@Override
	public void executeQuery(String sql, Object...params) {
		try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
			replaceParams(statement, params);
			statement.execute();
		} catch (SQLException ex) {
			center.logError(ex);
		}
	}
	
	@Override
	public void executeQuery(SqlQuery...queries) {
		try (Connection connection = data.getConnection()) {
			PreparedStatement[] statements = new PreparedStatement[queries.length - 1];
			for (int n = 0; n < queries.length; n++) {
				statements[n] = connection.prepareStatement(queries[n].statement());
				replaceParams(statements[n], queries[n].parameters());
				statements[n].execute();
				statements[n].close();
			}
		} catch (SQLException ex) {
			center.logError(ex);
		}
	}
	
	@Override
	public ResultSet[] selectQuery(SqlQuery...queries) {
		try (Connection connection = data.getConnection()) {
			PreparedStatement[] statements = new PreparedStatement[queries.length - 1];
			CachedRowSet[] results = new CachedRowSet[queries.length - 1];
			for (int n = 0; n < queries.length; n++) {
				statements[n] = connection.prepareStatement(queries[n].statement());
				replaceParams(statements[n], queries[n].parameters());
				results[n] = factory.createCachedRowSet();
				results[n].populate(statements[n].executeQuery());
				statements[n].close();
			}
			return results;
		} catch (SQLException ex) {
			throw new InternalStateException("Query retrieval failed!", ex);
		}
	}
	
	@Override
	public ResultSet selectQuery(String sql, Object...params) {
		try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
			replaceParams(statement, params);
			CachedRowSet results = factory.createCachedRowSet();
			results.populate(statement.executeQuery());
			return results;
		} catch (SQLException ex) {
			throw new InternalStateException("Query retrieval failed!", ex);
		}
	}

	@Override
	public void close() {
		stopConnection();
	}
	
	@Override
	public void refreshConfig(boolean first) {
		
		StorageMode mode;
		try {
			mode = StorageMode.fromString(center.config().getConfigString("storage.mode"));
		} catch (TypeParseException ex) {
			mode = StorageMode.HSQLDB;
			center.environment().logger().warning(DEFAULTING_TO_STORAGE_MODE);
		}
		
		if (StorageMode.MYSQL.equals(mode)) {
			settings = new LocalSettings(center.config());
		} else if (StorageMode.HSQLDB.equals(mode)) {
			settings = new RemoteSettings(center.config());
		}
		
		if (first || center.config().getConfigBoolean("storage.restart-on-reload")) {
			if (data != null) {
				data.close();
			}
			data = settings.loadDataSource();
		}
		
	}

}
