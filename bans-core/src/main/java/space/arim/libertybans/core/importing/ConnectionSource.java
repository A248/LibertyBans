/*
 * LibertyBans
 * Copyright © 2020 Anand Beh
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public interface ConnectionSource {

	Connection openConnection() throws SQLException;

	class InformativeErrorMessages implements ConnectionSource {

		private final ConnectionSource delegate;
		private final String importSourcePlugin;

		public InformativeErrorMessages(ConnectionSource delegate, String importSourcePlugin) {
			this.delegate = Objects.requireNonNull(delegate, "delegate");
			this.importSourcePlugin = Objects.requireNonNull(importSourcePlugin, "importSourcePlugin");
		}

		@Override
		public Connection openConnection() throws SQLException {
			try {
				return delegate.openConnection();
			} catch (SQLException ex) {
				throw new SQLException(
						"Failed to connect to database for " + importSourcePlugin + ". " +
								"Please ensure the settings in your import.yml are correct for that plugin",
						ex
				);
			}
		}
	}

}
