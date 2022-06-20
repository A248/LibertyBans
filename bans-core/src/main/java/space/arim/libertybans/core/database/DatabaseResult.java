/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

import space.arim.libertybans.core.database.jooq.JooqClassloading;

import java.util.concurrent.ForkJoinPool;

public final class DatabaseResult {

	private final StandardDatabase database;
	private final JooqClassloading jooqClassloading;
	private final boolean success;

	DatabaseResult(StandardDatabase database, JooqClassloading jooqClassloading, boolean success) {
		this.database = database;
		this.jooqClassloading = jooqClassloading;
		this.success = success;
	}

	public StandardDatabase database() {
		return database;
	}

	public boolean success() {
		return success;
	}

	void preinitializeJooqClasses() {
		ForkJoinPool.commonPool().execute(this.jooqClassloading::preinitializeClasses);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + System.identityHashCode(database);
		result = prime * result + (success ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DatabaseResult)) {
			return false;
		}
		DatabaseResult other = (DatabaseResult) object;
		return database == other.database && success == other.success;
	}
	
}
