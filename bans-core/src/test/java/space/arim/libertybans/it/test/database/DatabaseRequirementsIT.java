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

package space.arim.libertybans.it.test.database;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.core.database.DatabaseManager;
import space.arim.libertybans.core.database.DatabaseRequirements;
import space.arim.libertybans.core.database.Vendor;
import space.arim.libertybans.it.DatabaseInstance;
import space.arim.libertybans.it.InjectionInvocationContextProvider;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(InjectionInvocationContextProvider.class)
public class DatabaseRequirementsIT {

	@TestTemplate
	public void databaseVersion(DatabaseManager databaseManager,
								DatabaseInstance databaseInstance) throws SQLException {
		Vendor vendor = databaseInstance.getVendor();
		try (Connection connection = databaseManager.getInternal().getConnection()) {
			assertDoesNotThrow(
					new DatabaseRequirements(vendor, connection)::checkRequirementsAndYieldRetroSupport);
		}
	}

}
