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

package space.arim.libertybans.core.addon.checkpunish;

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.libertybans.core.addon.AddonConfig;

public interface CheckPunishConfig extends AddonConfig {

	@ConfKey("no-permission")
	@ConfDefault.DefaultString("&cSorry, you cannot use this.")
	Component noPermission();

	@ConfDefault.DefaultString("&cUsage: /libertybans checkpunish &e<id>")
	Component usage();

	@ConfKey("does-not-exist")
	@ConfDefault.DefaultString("&cThat punishment does not exist. It may be expired or have been revoked.")
	Component doesNotExist();

	@ConfDefault.DefaultStrings({
			"&7-- Listing punishment &e%ID%&7 --",
			"&7&oType: &f%TYPE%",
			"&7&oSubject: &e%VICTIM%",
			"&7&oReason: &7%REASON%",
			"&7&oOperator: &7%OPERATOR%",
			"&7&oTime Remaining: &7%TIME_REMAINING%"})
	ComponentText layout();

}
