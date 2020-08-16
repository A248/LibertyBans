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
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.DelayCalculators;
import space.arim.omnibus.util.concurrent.ScheduledWork;

import space.arim.libertybans.api.PunishmentDatabase;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.LibertyBansCore;

import space.arim.jdbcaesar.JdbCaesar;
import space.arim.jdbcaesar.adapter.DataTypeAdapter;
import space.arim.jdbcaesar.adapter.EnumNameAdapter;
import space.arim.jdbcaesar.builder.JdbCaesarBuilder;
import space.arim.jdbcaesar.transact.IsolationLevel;

public class Database implements PunishmentDatabase {

	private final LibertyBansCore core;
	private final JdbCaesar jdbCaesar;
	private final ExecutorService executor;
	
	private ScheduledWork<?> hyperSqlRefreshTask;
	
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
		HikariDataSource hikariDataSource = new HikariDataSource(hikariConf);

		DataTypeAdapter[] adapters = {
				new EnumNameAdapter<>(PunishmentType.class),
				new JdbCaesarHelper.VictimAdapter(),
				new EnumNameAdapter<>(Victim.VictimType.class),
				new JdbCaesarHelper.OperatorAdapter(),
				new JdbCaesarHelper.ScopeAdapter(core.getScopeManager())};
		jdbCaesar = new JdbCaesarBuilder()
				.databaseSource(new JdbCaesarHelper.HikariWrapper(hikariDataSource))
				.exceptionHandler((ex) -> {
					logger.error("Error while executing a database query", ex);
				})
				.defaultIsolation(IsolationLevel.REPEATABLE_READ)
				.addAdapters(adapters)
				.build();
	}
	
	void startRefreshTaskIfNecessary() {
		if (usesHyperSQL()) {
			synchronized (this) {
				hyperSqlRefreshTask = core.getResources().getEnhancedExecutor().scheduleRepeating(
						new IOUtils.HsqldbCleanerRunnable(core.getDatabaseManager(), this),
						Duration.ofHours(1L), DelayCalculators.fixedDelay());
			}
		}
	}
	
	void cancelRefreshTaskIfNecessary() {
		if (usesHyperSQL()) {
			synchronized (this) {
				hyperSqlRefreshTask.cancel();
			}
		}
	}
	
	private boolean usesHyperSQL() {
		 // See end of DatabaseSettings#getHikariConfg
		boolean usesMariaDb = ((JdbCaesarHelper.HikariWrapper) jdbCaesar().getDatabaseSource())
				.getHikariDataSource().getPoolName().contains("MariaDB");
		return !usesMariaDb;
	}
	
	void close() {
		try {
			jdbCaesar.getDatabaseSource().close();
		} catch (SQLException ex) {
			throw new AssertionError(ex);
		}
		executor.shutdown();
	}
	
	void closeCompletely() {
		if (usesHyperSQL()) {
			jdbCaesar().query("SHUTDOWN").voidResult().execute();
		}
		close();
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		if (logger.isDebugEnabled()) {
			logger.debug("Foreign caller acquiring connection on thread  {}", Thread.currentThread());
			if (TRACE_FOREIGN_CONNECTIONS) {
				logger.trace("Call trace as requested by space.arim.libertybans.traceForeignConnections=true", new Exception());
			}
		}
		return jdbCaesar.getDatabaseSource().getConnection();
	}
	
	public JdbCaesar jdbCaesar() {
		return jdbCaesar;
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
	
	public CentralisedFuture<?> executeAsync(Runnable command) {
		return core.getFuturesFactory().runAsync(command, executor);
	}
	
	public <T> CentralisedFuture<T> selectAsync(Supplier<T> supplier) {
		return core.getFuturesFactory().supplyAsync(supplier, executor);
	}

}
