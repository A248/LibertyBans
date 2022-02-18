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

package space.arim.libertybans.core.importing;

import space.arim.libertybans.core.database.InternalDatabase;
import space.arim.libertybans.core.database.execute.SQLTransactionalRunnable;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Batch SQL executor. NOT thread safe
 *
 */
class BatchOperationExecutor implements AutoCloseable {

	private final InternalDatabase database;

	private Connection connection;
	private int countInThisConnection;

	private static final int OPERATIONS_PER_CONNECTION = 400;

	BatchOperationExecutor(InternalDatabase database) {
		this.database = database;
	}

	void runOperation(SQLTransactionalRunnable operation) {
		try {
			runOperationChecked(operation);
		} catch (SQLException ex) {
			throw new ImportException(ex);
		}
	}

	private void runOperationChecked(SQLTransactionalRunnable operation) throws SQLException {
		openConnectionIfNecessary();

		/*
		 * Committing every time is a necessity. Queries cannot be batched together
		 * because otherwise the open transaction leads to deadlocks with other
		 * queries, such as uuid lookups for name-based operators.
		 *
		 * Committing is handled by #executeWithExistingConnection
		 */
		database.executeWithExistingConnection(connection, operation);

		if (++countInThisConnection == OPERATIONS_PER_CONNECTION) {
			countInThisConnection = 0;
			closeConnection();
		}
	}

	/*
	 * Lifecycle management
	 */

	private void openConnectionIfNecessary() throws SQLException {
		if (connection == null) {
			connection = database.getConnection();
		}
	}

	private void closeConnection() throws SQLException {
		try {
			connection.close();
		} finally {
			connection = null;
		}
	}

	@Override
	public void close() throws SQLException {
		if (connection != null) {
			closeConnection();
		}
	}
}
