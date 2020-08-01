/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.api.util.sql.HikariPoolSqlBackend;

import space.arim.libertybans.api.PunishmentDatabase;
import space.arim.libertybans.core.LibertyBansCore;

public class Database implements PunishmentDatabase {

	private final LibertyBansCore core;
	private final HikariPoolSqlBackend backend;
	private final ExecutorService executor;
	
	static final int REVISION_MAJOR = 1;
	static final int REVISION_MINOR = 0;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	private static final boolean TRACE_FOREIGN_CONNECTIONS;
	
	static {
		boolean traceForeign = false;
		try {
			traceForeign = Boolean.getBoolean("space.arim.libertybans.traceForeignConnections");
		} catch (SecurityException ignored) {}
		TRACE_FOREIGN_CONNECTIONS = traceForeign;
	}
	
	Database(LibertyBansCore core, HikariConfig hikariConf, int poolSize) {
		this.core = core;
		executor = Executors.newFixedThreadPool(poolSize, new IOUtils.ThreadFactoryImpl("LibertyBans-Database-"));
		backend = new HikariPoolSqlBackend(hikariConf);
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		if (logger.isDebugEnabled()) {
			logger.debug("Foreign caller acquiring connection on thread  {}", Thread.currentThread());
			if (TRACE_FOREIGN_CONNECTIONS) {
				logger.trace("Call trace as requested by space.arim.libertybans.traceForeignConnections=true", new Exception());
			}
		}
		return backend.getDataSource().getConnection();
	}
	
	public HikariPoolSqlBackend getBackend() {
		return backend;
	}

	@Override
	public int getMajorRevision() {
		return REVISION_MAJOR;
	}

	@Override
	public int getMinorRevision() {
		return REVISION_MINOR;
	}

	@Override
	public Executor getExecutor() {
		return executor;
	}
	
	CentralisedFuture<?> executeAsync(Runnable command) {
		return core.getFuturesFactory().runAsync(command, executor);
	}
	
	public <T> CentralisedFuture<T> selectAsync(Supplier<T> supplier) {
		return core.getFuturesFactory().supplyAsync(supplier, executor);
	}

}
