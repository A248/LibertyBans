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

package space.arim.libertybans.core.alts;

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;

@ConfHeader({
		"Messages for alt-checks and alt account notifications",
		"",
		"Before configuring this section, it is necessary to look at the address-enforcement",
		"settings in the main config.yml and understand the different kinds of alt detection.",
		"There is normal and strict detection."
})
public interface AltsSection {

	@SubSection
	Command command();

	@ConfHeader("Regarding the /alts command")
	interface Command {

		@ConfDefault.DefaultString("&cUsage: /alts &e<player>&c.")
		Component usage();

		@ConfDefault.DefaultString("&cYou may not check alts.")
		Component permission();

		@ConfKey("none-found")
		@ConfDefault.DefaultString("&7No alt accounts found")
		Component noneFound();

		@ConfComments("The message to display atop the alt check. Set to an empty string to disable")
		@ConfDefault.DefaultStrings({
				"&7Alt account report for &c&o%TARGET%&7 follows.",
				"&7Strong possibility - Same address as banned player.",
				"&7Mere possibility - Linked to banned player by common past address"})
		ComponentText header();

	}

	@SubSection
	@ConfKey("auto-show")
	AutoShow autoShow();

	interface AutoShow {

		@ConfComments("The message to display atop the alt check. Set to an empty string to disable")
		@ConfDefault.DefaultStrings({
				"&c&o%TARGET%&7 may be an alt account. Report follows.",
				"&7Strong possibility - Same address as banned player.",
				"&7Mere possibility - Linked to banned player by common past address"})
		ComponentText header();
	}

	@SubSection
	Formatting formatting();

	interface Formatting {

		@ConfComments({
				"How a single detected alt should be displayed",
				"Available variables:",
				"%DETECTION_KIND% - how the account was detected. Will be replaced by the normal or strict options.",
				"%ADDRESS% - the address in question which led to the detection",
				"%RELEVANT_USER% - the username of the other account, formatted according to the name-display option",
				"%RELEVANT_USERID% - the uuid of the other account",
				"%DATE_RECORDED% - the date the alt account was recorded"
		})
		@ConfDefault.DefaultString("%RELEVANT_USER% &r&7(per %ADDRESS%) at %DATE_RECORDED% - %DETECTION_KIND%")
		ComponentText layout();

		@ConfComments("The description for an alt account detected by normal detection.")
		@ConfDefault.DefaultString("&cStrong possibility")
		Component normal();

		@ConfComments("The description for an alt account detected by strict detection.")
		@ConfDefault.DefaultString("&eMere possibility")
		Component strict();

		@ConfKey("name-display")
		@SubSection
		NameDisplay nameDisplay();

		@ConfHeader({
				"In the alt-check layout, the username of the alt may be formatted depending upon whether it is banned",
				"For example, the usernames of banned alts may be colored red whereas alts not banned are green",
				"Variables: %USERNAME%"
		})
		interface NameDisplay {

			@ConfDefault.DefaultString("&c&o%USERNAME%")
			ComponentText banned();

			@ConfDefault.DefaultString("&e&o%USERNAME%")
			ComponentText muted();

			@ConfKey("not-punished")
			@ConfDefault.DefaultString("&f&o%USERNAME%")
			ComponentText notPunished();
		}

	}

}
