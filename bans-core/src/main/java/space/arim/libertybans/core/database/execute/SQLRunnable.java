/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

public interface SQLRunnable {

	default boolean isReadOnly() {
		return false;
	}

	void run(DSLContext context) throws RuntimeException;

	default SQLFunction<Void> runnableAsFunction() {
		class RunnableAsFunction implements SQLFunction<Void> {

			@Override
			public boolean isReadOnly() {
				return SQLRunnable.this.isReadOnly();
			}

			@Override
			public Void obtain(DSLContext context) throws RuntimeException {
				run(context);
				return null;
			}
		}
		return new RunnableAsFunction();
	}

	static SQLRunnable readOnly(SQLRunnable command) {
		return new SQLRunnable() {

			@Override
			public boolean isReadOnly() {
				return true;
			}

			@Override
			public void run(DSLContext context) throws RuntimeException {
				command.run(context);
			}
		};
	}
}
