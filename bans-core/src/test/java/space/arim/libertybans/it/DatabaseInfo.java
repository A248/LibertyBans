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

package space.arim.libertybans.it;

import java.util.Objects;

public final class DatabaseInfo {

	private final int port;
	private final String database;

	public DatabaseInfo() {
		this(-1, "");
	}

	public DatabaseInfo(int port, String database) {
		this.port = port;
		this.database = Objects.requireNonNull(database, "database");
	}

	public int port() {
		return port;
	}

	public String database() {
		return database;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DatabaseInfo that = (DatabaseInfo) o;
		return port == that.port && database.equals(that.database);
	}

	@Override
	public int hashCode() {
		int result = port;
		result = 31 * result + database.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "DatabaseInfo{" +
				"port=" + port +
				", database='" + database + '\'' +
				'}';
	}
}
