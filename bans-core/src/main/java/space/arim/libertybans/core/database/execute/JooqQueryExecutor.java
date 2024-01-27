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

package space.arim.libertybans.core.database.execute;

import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.core.database.jooq.JooqContext;
import space.arim.omnibus.util.ArraysUtil;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

public final class JooqQueryExecutor implements QueryExecutor {

	private final JooqContext jooqContext;
	private final DataSource dataSource;
	private final FactoryOfTheFuture futuresFactory;
	private final Executor threadPool;

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	public JooqQueryExecutor(JooqContext jooqContext, DataSource dataSource,
							 FactoryOfTheFuture futuresFactory, Executor threadPool) {
		this.jooqContext = Objects.requireNonNull(jooqContext, "jooqContext");
		this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
		this.futuresFactory = Objects.requireNonNull(futuresFactory, "futuresFactory");
		this.threadPool = Objects.requireNonNull(threadPool, "threadPool");
	}

	private static <E extends Throwable> E rollbackBeforeThrow(Connection connection, E reason) throws E {
		try {
			connection.rollback();
		} catch (SQLException suppressed) {
			reason.addSuppressed(suppressed);
		}
		throw reason;
	}

	private static DataAccessException unableToCommit(Connection connection, SQLException cause) {
		throw rollbackBeforeThrow(connection,
				new DataAccessException("Unable to commit (" + cause.getSQLState() + ')', cause));
	}

	private <R> R obtainUnfailing(SQLFunction<R> command) {
		try (Connection connection = dataSource.getConnection()) {
			if (command.isReadOnly()) {
				connection.setReadOnly(true);
			}
			DSLContext context = jooqContext.createContext(connection);

			R value;
			try {
				value = command.obtain(context);
			} catch (RuntimeException ex) {
				throw rollbackBeforeThrow(connection, ex);
			}
			try {
				connection.commit();
			} catch (SQLException ex) {
				throw unableToCommit(connection, ex);
			}
			return value;

		} catch (SQLException ex) {
			throw new DataAccessException("Miscellaneous failure (" + ex.getSQLState() + ')', ex);
		}
	}

	/**
	 * Determines whether the given SQL exception is caused by transaction serialization failure
	 *
	 * @param ex the sql exception
	 * @return true if caused by a transaction serialization failure
	 */
	private static boolean isSerializationFailure(SQLException ex) {
		/*
		HSQLDB - Serialization failure
		MariaDB and MySQL - ER_LOCK_DEADLOCK
		PostgreSQL and CockroachDB - serialization_failure
		 */
		return "40001".equals(ex.getSQLState());
	}

	private static void exponentialBackoff(int retry) {
		int sleepMs = 50 * ((int) Math.pow(2, retry)) + ThreadLocalRandom.current().nextInt(0, 100);
		try {
			Thread.sleep(sleepMs);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted during retry back-off", ex);
		}
	}

	private <R> R obtainWithRetry(int retryCount, SQLTransactionalFunction<R> command) {
		// Collect serialization failures and report them
		Exception[] serializationFailures = new Exception[0];

		try (Connection connection = dataSource.getConnection()) {
			DSLContext context = jooqContext.createContext(connection);

			for (int retry = 0; retry < retryCount; retry++) {
				if (retry != 0) {
					// This is not the first attempt
					connection.rollback();
					exponentialBackoff(retry);
				}
				RollbackTrackingTransaction transaction = new RollbackTrackingTransaction(context, connection);
				R value;
				try {
					value = command.obtain(context, transaction);
				} catch (DataAccessException ex) {
					SQLException rootCause;
					if ((rootCause = ex.getCause(SQLException.class)) != null && isSerializationFailure(rootCause)) {
						// Retry
						serializationFailures = ArraysUtil.expandAndInsert(serializationFailures, ex, 0);
						continue;
					}
					throw rollbackBeforeThrow(connection, ex);
				} catch (RuntimeException ex) {
					throw rollbackBeforeThrow(connection, ex);
				}
				if (transaction.wasNotRolledBack()) {
					try {
						connection.commit();
					} catch (SQLException ex) {
						if (isSerializationFailure(ex)) {
							// Retry
							serializationFailures = ArraysUtil.expandAndInsert(serializationFailures, ex, 0);
							continue;
						}
						throw unableToCommit(connection, ex);
					}
				}
				if (retry != 0) {
					logger.trace("Database operation succeeded after {} tries", retry);
					if (retry > retryCount / 2) {
						logger.info("Heavy contention detected on the database. Consider upping the retry count.");
					}
				}
				return value;
			}

		} catch (SQLException ex) {
			throw new DataAccessException("Unable to manage connection", ex);
		}
		DataAccessException failure = new DataAccessException(
				"Retry count exceeded. Here are the serialization failures in reverse order of occurrence.");
		for (Exception serializationFailure : serializationFailures) {
			failure.addSuppressed(serializationFailure);
		}
		throw failure;
	}

	@Override
	public void executeWithExistingConnection(Connection connection,
											  SQLTransactionalRunnable command) throws SQLException {

		DSLContext context = jooqContext.createContext(connection);
		RollbackTrackingTransaction transaction = new RollbackTrackingTransaction(context, connection);
		try {
			command.run(context, transaction);
		} catch (RuntimeException ex) {
			throw rollbackBeforeThrow(connection, ex);
		}
		if (transaction.wasNotRolledBack()) {
			try {
				connection.commit();
			} catch (SQLException ex) {
				throw unableToCommit(connection, ex);
			}
		}
	}

	@Override
	public CentralisedFuture<Void> execute(SQLRunnable command) {
		class RunnableAsFunction implements SQLFunction<Void> {

			@Override
			public boolean isReadOnly() {
				return command.isReadOnly();
			}

			@Override
			public Void obtain(DSLContext context) throws RuntimeException {
				command.run(context);
				return null;
			}
		}
		return query(new RunnableAsFunction());
	}

	@Override
	public <R> CentralisedFuture<R> query(SQLFunction<R> command) {
		return futuresFactory.supplyAsync(() -> obtainUnfailing(command), threadPool);
	}

	@Override
	public CentralisedFuture<Void> executeWithRetry(int retryCount, SQLTransactionalRunnable command) {
		class RunnableAsFunction implements SQLTransactionalFunction<Void> {

			@Override
			public Void obtain(DSLContext context, Transaction transaction) throws RuntimeException {
				command.run(context, transaction);
				return null;
			}
		}
		return queryWithRetry(retryCount, new RunnableAsFunction());
	}

	@Override
	public <R> CentralisedFuture<R> queryWithRetry(int retryCount, SQLTransactionalFunction<R> command) {
		return futuresFactory.supplyAsync(() -> obtainWithRetry(retryCount, command), threadPool);
	}

}
