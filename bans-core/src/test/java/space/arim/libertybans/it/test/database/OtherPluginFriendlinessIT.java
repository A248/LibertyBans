/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.bootstrap.RunState;
import space.arim.libertybans.it.DatabaseInstance;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetVendor;
import space.arim.libertybans.it.ThrowawayInstance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static space.arim.libertybans.core.database.Vendor.COCKROACH;
import static space.arim.libertybans.core.database.Vendor.MARIADB;
import static space.arim.libertybans.core.database.Vendor.MYSQL;
import static space.arim.libertybans.core.database.Vendor.POSTGRES;

@ExtendWith(InjectionInvocationContextProvider.class)
@ThrowawayInstance(manualStartup = true)
public class OtherPluginFriendlinessIT {

    private final BaseFoundation baseFoundation;

    public OtherPluginFriendlinessIT(BaseFoundation baseFoundation) {
        this.baseFoundation = baseFoundation;
    }

    @TestTemplate
    @SetVendor({MARIADB, MYSQL, POSTGRES, COCKROACH})
    public void preExistingTables(DatabaseInstance.Credential databaseCredential) {
        databaseCredential.executeStatement(
                "CREATE TABLE other_plugin (player VARCHAR(32) NOT NULL, points INT NOT NULL)");
        assertDoesNotThrow(baseFoundation::startup);
        assertEquals(RunState.RUNNING, baseFoundation.getRunState());
    }
}
