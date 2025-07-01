/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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
import space.arim.dazzleconf.annote.*;

@ConfHeader("Configuration for the /accounthistory command")
public interface AccountHistorySection {

	@ConfDefault.DefaultString("&cUsage: /accounthistory <delete|list>")
	Component usage();

	@SubSection
	Delete delete();

	@ConfHeader("Pertains to /accounthistory delete <user> <timestamp>")
	interface Delete {

		@ConfDefault.DefaultStrings({
				"&cUsage: /accounthistory delete <user> <timestamp>.",
				"&7The timestamp is in unix seconds and is usually obtained from /accounthistory list"})
		Component usage();

		@ConfDefault.DefaultString("&cYou may not delete recorded accounts.")
		Component permission();

		@ConfKey("no-such-account")
		@ConfDefault.DefaultString("&c%TARGET% has no recorded account for the specified timestamp.")
		ComponentText noSuchAccount();

		@ConfDefault.DefaultString("&7Successfully deleted the recorded account of &e%TARGET%")
		ComponentText success();
	}

	@SubSection
	Listing listing();

	@ConfHeader("Regards /accounthistory list")
	interface Listing extends AccountListFormatting {

		@ConfDefault.DefaultString("&cUsage: /accounthistory list <user|ip> [page]")
		Component usage();

		@ConfDefault.DefaultString("&cYou may not list recorded accounts.")
		Component permission();

		@ConfKey("none-found")
		@ConfDefault.DefaultString("&7Page does not exist.")
		ComponentText noneFound();

		@Override
		@ConfComments({
				"The message to display before the account listing. Set to an empty string to disable",
				"Available variables:",
				"%TARGET% - the target user",
				"%PAGE% - the current page number",
				"%NEXTPAGE% - the next page number",
				"%NEXTPAGE_KEY% - a code which if used with the command, shows the next page",
				"%LASTPAGE% - the last page number",
				"%LASTPAGE_KEY% - a code which if used with the command, shows the last page"
		})
		@ConfDefault.DefaultString("&7Known accounts report for &c&o%TARGET%&7 follows.")
		ComponentText header();

		@ConfComments({
				"How a single recorded account should be displayed",
				"Available variables, in addition to header variables:",
				"%USERNAME% - the username the player connected with",
				"%ADDRESS% - the address the player connected with",
				"%DATE_RECORDED% - the date the join was recorded",
				"%DATE_RECORDED_RAW% - the raw timestamp the join was recorded"
		})
		@ConfDefault.DefaultString("%USERNAME% &r&7(on %ADDRESS%) at %DATE_RECORDED% (%DATE_RECORDED_RAW%)||ttp:&7Click to delete this stored account||cmd:/accounthistory delete %TARGET% %DATE_RECORDED_RAW%")
		ComponentText layout();

		@ConfComments("Amount of accounts to display per page")
		@ConfKey("per-page")
		@IntegerRange(min = 1)
		@ConfDefault.DefaultInteger(10)
		int perPage();

		@Override
		@ConfComments("The separator between list entries")
		@ConfDefault.DefaultString("\n")
		Component separator();

		@Override
		@ConfComments({
				"How to format the footer. This is sent after every page.",
				"Available variables are the same as for the header."
		})
		@ConfDefault.DefaultString("&7<Next Page>||ttp:Click for next page||cmd:/libertybans accounthistory %TARGET% %NEXTPAGE_KEY%")
		ComponentText footer();

	}
}
