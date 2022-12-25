/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.core.database;

import com.zaxxer.hikari.HikariDataSource;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.database.PunishmentDatabase;
import space.arim.libertybans.bootstrap.plugin.PluginInfo;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.database.execute.SQLFunction;
import space.arim.libertybans.core.database.execute.SQLRunnable;
import space.arim.libertybans.core.database.execute.SQLTransactionalFunction;
import space.arim.libertybans.core.database.execute.SQLTransactionalRunnable;
import space.arim.libertybans.core.database.sql.TableForType;
import space.arim.libertybans.core.service.Time;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.DelayCalculators;
import space.arim.omnibus.util.concurrent.EnhancedExecutor;
import space.arim.omnibus.util.concurrent.ScheduledTask;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static space.arim.libertybans.core.schema.Tables.PUNISHMENTS;

public final class StandardDatabase implements InternalDatabase, AutoCloseable {

	private final DatabaseManager manager;
	private final Vendor vendor;
	private final HikariDataSource dataSource;
	private final QueryExecutor queryExecutor;
	private final ExecutorService threadPool;
	private final PunishmentDatabase external = new External();

	private ScheduledTask expirationRefreshTask;
	private ScheduledTask synchronizationPollTask;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	StandardDatabase(DatabaseManager manager, Vendor vendor,
					 HikariDataSource dataSource, QueryExecutor queryExecutor, ExecutorService threadPool) {
		this.manager = manager;
		this.vendor = vendor;
		this.dataSource = dataSource;
		this.queryExecutor = queryExecutor;
		this.threadPool = threadPool;
	}

	/*
	 * Lifecycle
	 * 
	 * Guarded by the global lock on BaseFoundation lifecycle events
	 */

	void startTasks(Time time) {
		EnhancedExecutor enhancedExecutor = manager.enhancedExecutor();
		expirationRefreshTask = enhancedExecutor.scheduleRepeating(
				new RefreshTaskRunnable(manager, this, time),
				Duration.ofHours(3L),
				DelayCalculators.fixedDelay()
		);
		var synchronizationConf = manager.configs().getSqlConfig().synchronization();
		if (synchronizationConf.enabled()) {
			synchronizationPollTask = enhancedExecutor.scheduleRepeating(
					manager.globalEnforcement(),
					Duration.ofMillis(synchronizationConf.pollRateMillis()),
					DelayCalculators.fixedDelay()
			);
		}
	}

	void cancelTasks() {
		expirationRefreshTask.cancel();
		if (synchronizationPollTask != null) {
			synchronizationPollTask.cancel();
			synchronizationPollTask = null;
		}
	}

	@Override
	public void close() {
		threadPool.shutdown();
		try {
			boolean terminated = threadPool.awaitTermination(5L, TimeUnit.SECONDS);
			if (!terminated) {
				logger.warn("Reached timeout while waiting for thread pool");
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			logger.warn("Interrupted while waiting for thread pool", ex);
		}
		dataSource.close();
	}

	void closeCompletely() {
		if (getVendor() == Vendor.HSQLDB) {
			execute((context) -> context.query("SHUTDOWN").execute()).join();
		}
		close();
	}
	
	@Override
	public PunishmentDatabase asExternal() {
		return external;
	}
	
	@Override
	public Vendor getVendor() {
		return vendor;
	}

	@Override
	public void executeWithExistingConnection(Connection connection, SQLTransactionalRunnable command) throws SQLException {
		queryExecutor.executeWithExistingConnection(connection, command);
	}

	@Override
	public CentralisedFuture<Void> execute(SQLRunnable command) {
		return queryExecutor.execute(command);
	}

	@Override
	public <R> CentralisedFuture<R> query(SQLFunction<R> command) {
		return queryExecutor.query(command);
	}

	@Override
	public CentralisedFuture<Void> executeWithRetry(int retryCount, SQLTransactionalRunnable command) {
		return queryExecutor.executeWithRetry(retryCount, command);
	}

	@Override
	public <R> CentralisedFuture<R> queryWithRetry(int retryCount, SQLTransactionalFunction<R> command) {
		return queryExecutor.queryWithRetry(retryCount, command);
	}

	@Override
	public void clearExpiredPunishments(DSLContext context, PunishmentType type, Instant currentTime) {
		assert type != PunishmentType.KICK;
		var dataTable = new TableForType(type).dataTable();
		context
				.deleteFrom(dataTable.table())
				.where(dataTable.id().in(context
						.select(PUNISHMENTS.ID)
						.from(PUNISHMENTS)
						.where(PUNISHMENTS.END.notEqual(Instant.MAX))
						.and(PUNISHMENTS.END.lessThan(currentTime))
				)).execute();
	}

	@Override
	public void truncateAllTables() {
		execute((context) -> {
			for (Table<?> table : DatabaseConstants.allTables(DatabaseConstants.TableOrder.REFERENTS_LAST)) {
				context.deleteFrom(table).execute();
			}
		}).join();
	}

	@Override
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	private class External implements PunishmentDatabase {

		@Override
		public Connection getConnection() throws SQLException {
			logger.debug("Foreign caller acquiring connection");
			return StandardDatabase.this.getConnection();
		}

		@Override
		public int getMajorRevision() {
			return PluginInfo.DATABASE_REVISION_MAJOR;
		}

		@Override
		public int getMinorRevision() {
			return PluginInfo.DATABASE_REVISION_MINOR;
		}

		@Override
		public Executor getExecutor() {
			return threadPool;
		}

	}

}
