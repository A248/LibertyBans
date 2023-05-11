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

package space.arim.libertybans.core.addon.checkuser;

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.libertybans.core.addon.AddonConfig;

public interface CheckUserConfig extends AddonConfig {

	@ConfKey("no-permission")
	@ConfDefault.DefaultString("&cYou do not have permission to check user data")
	Component noPermission();

	@ConfDefault.DefaultString("&cUsage: /libertybans checkuser <player>")
	Component usage();

	@ConfKey("player-does-not-exist")
	@ConfDefault.DefaultString("&cThat player does not exist")
	Component doesNotExist();

	@ConfKey("punishment-does-not-exist")
	@ConfDefault.DefaultString("&cThat player doesn't have any active punishment.")
	Component noPunishment();

	@ConfDefault.DefaultStrings({
			"&7Active punishment for player &e%VICTIM%",
			"&7Type: &e%TYPE%",
			"&7Reason: &e%REASON%",
			"&7Operator: &e%OPERATOR%",
			"Time remaining: &e%TIME_REMAINING%",
	})
	ComponentText layout();
}
