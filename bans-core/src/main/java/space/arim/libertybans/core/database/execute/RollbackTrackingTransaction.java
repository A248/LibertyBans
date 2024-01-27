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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.Objects;

final class RollbackTrackingTransaction implements Transaction {

	private final DSLContext context;
	private final Connection connection;
	private boolean rolledBack;
	private int savepointCounter;

	RollbackTrackingTransaction(DSLContext context, Connection connection) {
		this.context = Objects.requireNonNull(context, "context");
		this.connection = Objects.requireNonNull(connection, "connection");
	}

	boolean wasNotRolledBack() {
		return !rolledBack;
	}

	@Override
	public void setIsolation(int level) {
		try {
			connection.setTransactionIsolation(level);
		} catch (SQLException ex) {
			throw new DataAccessException("Failed to set isolation", ex);
		}
	}

	@Override
	public void rollback() {
		try {
			connection.rollback();
		} catch (SQLException ex) {
			throw new DataAccessException("Failed to rollback", ex);
		} finally {
			rolledBack = true;
		}
	}

	@Override
	public <R> R executeNested(SQLTransactionalFunction<R> command) {
		R value;
		try {
			Savepoint savepoint = connection.setSavepoint("Savepoint-" + savepointCounter++);
			NestedTransaction nested = new NestedTransaction(savepoint);
			value = command.obtain(context, nested);
			if (!nested.rolledBack) {
				connection.releaseSavepoint(savepoint);
			}
		} catch (SQLException ex) {
			throw new DataAccessException("Failed to manage nested transaction", ex);
		}
		return value;
	}

	@Override
	public String toString() {
		return "RollbackTrackingTransaction{" +
				"context=" + context +
				", connection=" + connection +
				", rolledBack=" + rolledBack +
				'}';
	}

	private final class NestedTransaction implements Transaction {

		private final Savepoint savepoint;
		private boolean rolledBack;

		private NestedTransaction(Savepoint savepoint) {
			this.savepoint = Objects.requireNonNull(savepoint, "savepoint");
		}

		@Override
		public void setIsolation(int level) {
			throw new UnsupportedOperationException("Cannot set isolation levels in nested transactions");
		}

		@Override
		public void rollback() {
			try {
				connection.rollback(savepoint);
				rolledBack = true;
			} catch (SQLException ex) {
				throw new DataAccessException("Failed to rollback to savepoint", ex);
			}
		}

		@Override
		public <R> R executeNested(SQLTransactionalFunction<R> command) {
			return RollbackTrackingTransaction.this.executeNested(command);
		}

		@Override
		public String toString() {
			return "NestedTransaction{" +
					"savepoint=" + savepoint +
					'}';
		}
	}
}
