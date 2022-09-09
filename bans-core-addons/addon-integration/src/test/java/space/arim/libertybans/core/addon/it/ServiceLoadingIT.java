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

package space.arim.libertybans.core.addon.it;

import org.junit.jupiter.api.Test;
import space.arim.libertybans.core.addon.AddonLoader;
import space.arim.libertybans.core.addon.checkpunish.CheckPunishModule;
import space.arim.libertybans.core.addon.checkuser.CheckUserModule;
import space.arim.libertybans.core.addon.staffrollback.StaffRollbackModule;
import space.arim.libertybans.core.addon.warnactions.WarnActionsModule;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServiceLoadingIT {

	@Test
	public void loadAddons() {
		assertEquals(
				Set.of(
						new CheckPunishModule(), new CheckUserModule(),
						new StaffRollbackModule(), new WarnActionsModule()
				),
				assertDoesNotThrow(AddonLoader::loadAddonBindModules)
		);
	}

}
