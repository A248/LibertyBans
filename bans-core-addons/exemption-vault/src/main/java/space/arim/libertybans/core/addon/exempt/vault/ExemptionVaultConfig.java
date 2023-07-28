/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

package space.arim.libertybans.core.addon.exempt.vault;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.libertybans.core.addon.AddonConfig;

public interface ExemptionVaultConfig extends AddonConfig {

	@ConfKey("max-level-to-scan-for")
	@ConfComments({
			"The maximum exemption level you intend to grant your staff members."
	})
	@ConfDefault.DefaultInteger(50)
	int maxLevelToScanFor();

	@ConfKey("permission-check-thread-context")
	@ConfComments({
			"This option provides technical compatibility with different permissions plugins.",
			"The default setting of 'REQUIRE_MAIN_THREAD' is contractually correct. However, some plugins require a different setting.",
			"",
			"Permission plugins and their required settings:",
			"- LuckPerms => USE_ASYNC_FOR_OFFLINE_PLAYERS or REQUIRE_ASYNC",
			"- PermissionsEx => REQUIRE_MAIN_THREAD",
			"If you don't see your permission plugin listed here, ask and we will investigate for you.",
			"",
			"A technical explanation of each mode:",
			"- RUN_ANYWHERE allows permission checks to happen on any thread",
			"- REQUIRE_MAIN_THREAD runs all permission checks on the main thread",
			"- REQUIRE_ASYNC runs checks asynchronously",
			"- USE_ASYNC_FOR_OFFLINE_PLAYERS runs checks for offline players asynchronously, and checks for online players on the main thread",
	})
	@ConfDefault.DefaultString("REQUIRE_MAIN_THREAD")
	PermissionCheckThreadContext permissionCheckThreadContext();

	enum PermissionCheckThreadContext {
		RUN_ANYWHERE,
		REQUIRE_MAIN_THREAD,
		REQUIRE_ASYNC,
		USE_ASYNC_FOR_OFFLINE_PLAYERS
	}

}
