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
import java.util.logging.Level;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

import com.zaxxer.hikari.HikariDataSource;

import space.arim.bans.ArimBans;
import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.internal.sql.SqlQuery.Query;

import space.arim.universal.util.collections.CollectionsUtil;

import space.arim.api.sql.ExecutableQuery;
import space.arim.api.util.StringsUtil;

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
			statement.setObject(n + 1, parameters[n]);
		}
	}
	
	@Override
	public String getStorageModeName() {
		return settings.toString();
	}
	
	@Override
	public boolean enabled() {
		return (data != null) ? !data.isClosed() : false;
	}
	
	private void execQuery(ExecutableQuery...queries) throws SQLException {
		if (queries.length == 0) {
			return;
		}
		try (Connection connection = data.getConnection()) {
			PreparedStatement[] statements = new PreparedStatement[queries.length];
			for (int n = 0; n < queries.length; n++) {
				statements[n] = connection.prepareStatement(queries[n].statement());
				replaceParams(statements[n], queries[n].parameters());
				statements[n].execute();
				statements[n].close();
			}
		}
	}
	
	private void execQuery(String query, Object...params) throws SQLException {
		center.logs().log(Level.CONFIG, "Executing query [" + query + "] with params [" + params + "]");
		try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
			replaceParams(statement, params);
			statement.execute();
		}
	}
	
	private ResultSet[] selQuery(ExecutableQuery...queries) throws SQLException {
		try (Connection connection = data.getConnection()) {
			PreparedStatement[] statements = new PreparedStatement[queries.length];
			CachedRowSet[] results = new CachedRowSet[queries.length];
			for (int n = 0; n < queries.length; n++) {
				statements[n] = connection.prepareStatement(queries[n].statement());
				replaceParams(statements[n], queries[n].parameters());
				results[n] = factory.createCachedRowSet();
				results[n].populate(statements[n].executeQuery());
				statements[n].close();
			}
			return results;
		}
	}
	
	private ResultSet selQuery(String query, Object...params) throws SQLException {
		try (Connection connection = data.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
			replaceParams(statement, params);
			CachedRowSet results = factory.createCachedRowSet();
			results.populate(statement.executeQuery());
			return results;
		}
	}
	
	@Override
	public void executeQuery(ExecutableQuery...queries) throws SQLException {
		center.logs().log(Level.CONFIG, "Executing externally-called queries [" + StringsUtil.concat(CollectionsUtil.convertAll(queries, (query) -> query.toString()), ',') + "]");
		execQuery(queries);
	}
	
	@Override
	public void executeQuery(String query, Object...params) throws SQLException {
		center.logs().log(Level.CONFIG, "Executing query [" + query + "] with parameters [" + StringsUtil.concat(CollectionsUtil.convertAll(params, (param) -> param.toString()), ',') + "]");
		execQuery(query, params);
	}
	
	@Override
	public ResultSet[] selectQuery(ExecutableQuery...queries) throws SQLException {
		center.logs().log(Level.CONFIG, "Executing selection queries [" + StringsUtil.concat(CollectionsUtil.convertAll(queries, (query) -> query.toString()), ',') + "]");
		return selQuery(queries);
	}
	
	@Override
	public ResultSet selectQuery(String query, Object...params) throws SQLException {
		center.logs().log(Level.CONFIG, "Executing selection query [" + query + "] with parameters [" + StringsUtil.concat(CollectionsUtil.convertAll(params, (param) -> param.toString()), ',') + "]");
		return selQuery(query, params);
	}
	
	@Override
	public void executeQuery(SqlQuery...queries) {
		center.logs().log(Level.CONFIG, "Executing queries [" + StringsUtil.concat(CollectionsUtil.convertAll(queries, (query) -> query.toString()), ',') + "]");
		try {
			execQuery(CollectionsUtil.convertAll(queries, (query) -> query.convertToExecutable(settings)));
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
	}
	
	@Override
	public void executeQuery(Query query, Object...params) {
		center.logs().log(Level.CONFIG, "Executing query [" + query + "] with parameters [" + StringsUtil.concat(CollectionsUtil.convertAll(params, (param) -> param.toString()), ',') + "]");
		try {
			execQuery(query.eval(settings), params);
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
	}
	
	@Override
	public ResultSet[] selectQuery(SqlQuery...queries) {
		center.logs().log(Level.CONFIG, "Executing selection queries [" + StringsUtil.concat(CollectionsUtil.convertAll(queries, (query) -> query.toString()), ',') + "]");
		try {
			return selQuery(CollectionsUtil.convertAll(queries, (query) -> query.convertToExecutable(settings)));
		} catch (SQLException ex) {
			throw new InternalStateException("Query retrieval failed!", ex);
		}
	}
	
	@Override
	public ResultSet selectQuery(Query query, Object...params) {
		center.logs().log(Level.CONFIG, "Executing selection query [" + query + "] with parameters [" + StringsUtil.concat(CollectionsUtil.convertAll(params, (param) -> param.toString()), ',') + "]");
		try {
			return selQuery(query.eval(settings), params);
		} catch (SQLException ex) {
			throw new InternalStateException("Query retrieval failed!", ex);
		}
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
