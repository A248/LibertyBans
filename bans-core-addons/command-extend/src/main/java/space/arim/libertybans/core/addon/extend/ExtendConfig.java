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

package space.arim.libertybans.core.addon.extend;

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.libertybans.core.addon.AddonConfig;

public interface ExtendConfig extends AddonConfig {

	@ConfDefault.DefaultString("&cUsage: /libertybans extend <id> <time>")
	Component usage();

	@ConfKey("no-permission")
	@ConfDefault.DefaultString("&cSorry, you cannot use this.")
	Component noPermission();

	@ConfKey("not-found")
	@ConfDefault.DefaultString("&7No punishment exists with ID &e%ID%&7.")
	ComponentText notFound();

	@ConfKey("cannot-extend-kicks")
	@ConfDefault.DefaultString("&cThe punishment with ID &e%ID%&c is a kick so it cannot be extended.")
	ComponentText cannotExtendKicks();

	@ConfKey("invalid-duration")
	@ConfDefault.DefaultString("&e%DURATION_ARG%&c is not a valid duration.")
	ComponentText invalidDuration();

	@ConfDefault.DefaultString("&7Extended punishment &e%ID%&7. It now ends on &e%END_DATE%&7.")
	ComponentText success();

}
