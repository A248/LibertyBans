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

package space.arim.libertybans.core.addon.shortcutreasons;

import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.libertybans.core.addon.AddonConfig;

import java.util.Map;

public interface ShortcutReasonsConfig extends AddonConfig {

	@ConfKey("shortcut-identifier")
	@ConfComments("The special prefix used to identify a reason shortcut when executing commands")
	@ConfDefault.DefaultString("#")
	String shortcutIdentifier();

	@ConfKey("does-not-exist")
	@ConfComments("If a staff member specifies an invalid shortcut, the command will be halted to prevent mistakes.")
	@ConfDefault.DefaultString("&cThere is no shortcut reason configured for &e%SHORTCUT_ARG%&c.")
	ComponentText doesNotExist();

	@ConfComments({"The shortcuts themselves.",
			"Remember to wrap apostrophes and other special characters inside quotation marks."})
	@ConfDefault.DefaultMap({
			"hacking", "You are punished for hacking",
			"chat_spam", "Please don't spam the chat"
	})
	Map<String, String> shortcuts();

}
