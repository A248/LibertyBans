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
import java.util.UUID;
import java.util.logging.Level;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;

import com.zaxxer.hikari.HikariDataSource;

import space.arim.bans.ArimBans;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.internal.sql.BasicQuery.PreQuery;

import space.arim.universal.util.collections.CollectionsUtil;
import space.arim.universal.util.function.erring.ErringLazySingleton;

import space.arim.api.sql.ExecutableQuery;
import space.arim.api.util.StringsUtil;

public class Sql implements SqlMaster {

	private final ArimBans center;

	private static final String DEFAULTING_TO_STORAGE_MODE = "Invalid storage mode specified! Defaulting to FILE...";
	
	private HikariDataSource data;

	private SqlSettings settings;
	
	private final ErringLazySingleton<RowSetFactory, SQLException> factory = new ErringLazySingleton<RowSetFactory, SQLException>(() -> RowSetProvider.newFactory());

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
	
	private static Object convert(Object obj) {
		if (obj instanceof Subject) {
			return ((Subject) obj).deserialise();
		} else if (obj instanceof UUID) {
			return obj.toString().replace("-", "");
		} else if (obj instanceof PunishmentType) {
			return ((PunishmentType) obj).deserialise();
		}
		return obj;
	}
	
	private static PreparedStatement replaceParameters(PreparedStatement statement, Object...parameters) throws SQLException {
		for (int n = 0; n < parameters.length; n++) {
			statement.setObject(n + 1, convert(parameters[n]));
		}
		return statement;
	}
	
	private CachedRowSet executeAndCache(PreparedStatement statement) throws SQLException {
		if (!statement.execute()) {
			return null;
		}
		CachedRowSet results = factory.get().createCachedRowSet();
		results.populate(statement.getResultSet());
		return results;
	}
	
	private PreparedStatement prepareStatement(Connection connection, ExecutableQuery query) throws SQLException {
		center.logs().log(Level.CONFIG, "Executing query " + query.toString());
		return replaceParameters(connection.prepareStatement(query.statement()), query.parameters());
	}
	
	@Override
	public ResultSet[] execute(ExecutableQuery...queries) throws SQLException {
		center.logs().log(Level.CONFIG, "Executing externally-called queries [" + StringsUtil.concat(CollectionsUtil.convertAllToString(queries), ',') + "]");
		try (Connection connection = data.getConnection()) {
			PreparedStatement[] statements = new PreparedStatement[queries.length];
			CachedRowSet[] results = new CachedRowSet[queries.length];
			for (int n = 0; n < queries.length; n++) {
				statements[n] = prepareStatement(connection, queries[n]);
				results[n] = executeAndCache(statements[n]);
				statements[n].close();
			}
			return results;
		}
	}
	
	@Override
	public ResultSet[] execute(Query...queries) {
		center.logs().log(Level.CONFIG, "Executing queries [" + StringsUtil.concat(CollectionsUtil.convertAllToString(queries), ',') + "]");
		try (Connection connection = data.getConnection()) {
			PreparedStatement[] statements = new PreparedStatement[queries.length];
			CachedRowSet[] results = new CachedRowSet[queries.length];
			for (int n = 0; n < queries.length; n++) {
				statements[n] = prepareStatement(connection, queries[n].convertToExecutable(settings));
				results[n] = executeAndCache(statements[n]);
				statements[n].close();
			}
			return results;
		} catch (SQLException ex) {
			center.logs().logError(ex);
			throw new InternalStateException("Query execution failed!", ex);
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
		case "remote":
		case "external":
			return new RemoteSettings(center.config());
		default:
			center.logs().log(Level.WARNING, DEFAULTING_TO_STORAGE_MODE);
			return new LocalSettings(center.config());
		}
	}
	
	@Override
	public void refreshConfig(boolean first) {
		
		settings = parseBackend(center.config().getConfigString("storage.mode"));
		center.logs().log(Level.FINE, "Set data backend " + settings);
		
		if (first || center.config().getConfigBoolean("storage.restart-on-reload")) {
			center.logs().log(Level.CONFIG, "Loading data backend " + settings);
			if (data != null) {
				data.close();
			}
			data = settings.loadDataSource();
			execute(new BasicQuery(PreQuery.CREATE_TABLE_ACTIVE), new BasicQuery(PreQuery.CREATE_TABLE_HISTORY), new BasicQuery(PreQuery.CREATE_TABLE_CACHE));
		}
		
	}

}
