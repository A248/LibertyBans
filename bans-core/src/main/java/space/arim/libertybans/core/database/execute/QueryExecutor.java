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

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.sql.Connection;
import java.sql.SQLException;

public interface QueryExecutor {

	int DEFAULT_RETRIES = 5;

	/**
	 * Executes a command using an existing connection. When the operation is complete,
	 * the transaction will be committed
	 *
	 * @param command the operation to run
	 */
	void executeWithExistingConnection(Connection connection, SQLTransactionalRunnable command) throws SQLException;

	/**
	 * Executes a command. Because this will not retry on transaction
	 * serialization failure, it should only be used for operations which
	 * will not fail due to transaction serialization.
	 *
	 * @param command the operation to run
	 * @return a future completed once the SQL is executed
	 */
	CentralisedFuture<Void> execute(SQLRunnable command);

	/**
	 * Executes a command. Because this will not retry on transaction
	 * serialization failure, it should only be used for operations which
	 * will not fail due to transaction serialization.
	 *
	 * @param command the operation to run
	 * @param <R> the return type
	 * @return a future completed once the SQL is executed
	 */
	<R> CentralisedFuture<R> query(SQLFunction<R> command);

	/**
	 * Retrieves a command until it succeeds
	 *
	 * @param retryCount the amount of times to retry
	 * @param command the operation to run
	 * @return a future completed once the command is successfully run
	 */
	CentralisedFuture<Void> executeWithRetry(int retryCount, SQLTransactionalRunnable command);

	/**
	 * Retrieves a command until it succeeds
	 *
	 * @param command the operation to run
	 * @return a future completed once the command is successfully run
	 */
	default CentralisedFuture<Void> executeWithRetry(SQLTransactionalRunnable command) {
		return executeWithRetry(DEFAULT_RETRIES, command);
	}

	/**
	 * Retrieves a command until it succeeds
	 *
	 * @param retryCount the amount of times to retry
	 * @param command the operation to run
	 * @param <R> the return value
	 * @return a future completed once the command is successfully runt
	 */
	<R> CentralisedFuture<R> queryWithRetry(int retryCount, SQLTransactionalFunction<R> command);

	/**
	 * Retrieves a command until it succeeds
	 *
	 * @param command the operation to run
	 * @param <R> the return value
	 * @return a future completed once the command is successfully runt
	 */
	default <R> CentralisedFuture<R> queryWithRetry(SQLTransactionalFunction<R> command) {
		return queryWithRetry(DEFAULT_RETRIES, command);
	}
}
