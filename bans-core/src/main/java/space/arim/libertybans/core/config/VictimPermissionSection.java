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

package space.arim.libertybans.core.config;

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfKey;

/**
 * Section for permission messages, used for both punishment additions and removals
 *
 */
public interface VictimPermissionSection {

	@ConfDefault.DefaultString("&cYou may not do this to players.")
	Component uuid();

	@ConfKey("ip-address")
	@ConfDefault.DefaultString("&cYou may not do this to IP addresses.")
	Component ipAddress();

	@ConfDefault.DefaultString("&cYou may not do this to players and their IP addresses.")
	Component both();

	interface WithDuration extends VictimPermissionSection {

		@ConfDefault.DefaultString("&cYou may not do this for &e%DURATION%&c.")
		ComponentText duration();

	}

}
