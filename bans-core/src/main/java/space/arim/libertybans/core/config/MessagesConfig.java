/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.config;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;

import space.arim.libertybans.api.PunishmentType;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault.DefaultBoolean;
import space.arim.dazzleconf.annote.ConfDefault.DefaultMap;
import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfDefault.DefaultStrings;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.core.alts.AccountHistorySection;
import space.arim.libertybans.core.alts.AltsSection;

@ConfHeader({
		"",
		"Messages configuration",
		"",
		"",
		"In most cases, the variables inside the default messages are those available",
		"in that specific message. The exception to this is messages which are related",
		"to a certain punishment.",
		"",
		"When message has an associated punishment, multiple variables are available:",
		"",
		"%ID% - punishment ID number",
		"%TYPE% - punishment type, e.g. 'Ban'",
		"%TYPE_VERB% - punishment type as a verb, e.g. 'Banned'",
		"%VICTIM% - display name of the victim of the punishment",
		"%VICTIM_ID% - internal identifier of victim",
		"%OPERATOR% - display name of the staff member who made the punishment",
		"%OPERATOR_ID% - internal identifier of the operator",
		"%UNOPERATOR% - staff member undoing the punishment. available only when the punishment is undone",
		"%UNOPERATOR_ID% - internal identifier of staff member undoing the punishment",
		"%REASON% - reason for the punishment",
		"%SCOPE% - scope of the punishment",
		"%DURATION% - original duration (how long the punishment was made for)",
		"%START_DATE% - the date the punishment was created",
		"%TIME_PASSED% - the time since the punishment was created",
		"%TIME_PASSED_SIMPLE% - the time since the punishment was created, rounded to the biggest time unit possible (e.g. 2 months instead of 1 month 23 days)",
		"%END_DATE% - the date the punishment will end, or formatting.permanent-display.absolute for permanent punishments",
		"%TIME_REMAINING% - the time until the punishment ends, or formatting.permanent-display.relative for permanent punishments",
		"%TIME_REMAINING_SIMPLE% - the time until the punishment ends, rounded to the biggest time unit possible (e.g. 10 days instead of 9 days 13 hours)",
		"%HAS_EXPIRED% - Shows if a punishments duration has run out. Does not check if the punishment is revoked!",
		"",
		"The following variables have limited availability:",
		"%TARGET% - the original target argument of a command. For example, in '/ipban Player1', %TARGET% is Player1",
		"%NEXTPAGE% - the number of the next page of a list like history",
		"%PREVIOUSPAGE% - the number of the previous page of a list like history",
		"",
		""})
public interface MessagesConfig {
	
	@SubSection
	All all();

	interface All {
		
		@ConfKey("prefix.enable")
		@ConfComments("If enabled, all messages will be prefixed")
		@DefaultBoolean(true)
		boolean enablePrefix();
		
		@ConfKey("prefix.value")
		@ConfComments("The prefix to use")
		@DefaultString("&6&lLibertyBans &r&8»&7 ")
		Component rawPrefix();

		default Component prefix() {
			return (enablePrefix()) ? rawPrefix() : Component.empty();
		}
		
		@ConfKey("base-permission-message")
		@ConfComments("If a player types /libertybans but does not have the permission 'libertybans.commands', "
				+ "this is the denial message")
		@DefaultString("&cYou may not use this.")
		Component basePermissionMessage();
		
		@ConfKey("not-found")
		@ConfComments("When issuing commands, if the specified player or IP was not found, what should the error message be?")
		@SubSection
		NotFound notFound();
		
		interface NotFound {
			
			@DefaultString("&c&o%TARGET%&r&7 was not found online or offline.")
			ComponentText player();
			
			@DefaultString("&c&o%TARGET%&r&7 is not a valid uuid.")
			ComponentText uuid();

			@ConfKey("player-or-address")
			@DefaultString("&c&o%TARGET%&r&7 was not found online or offline, and is not a valid IP address.")
			ComponentText playerOrAddress();

		}
		
		@DefaultString("&cUnknown sub command. Displaying usage:")
		Component usage();
		
	}
	
	@SubSection
	Admin admin();
	
	interface Admin {
		
		@ConfKey("no-permission")
		@DefaultString("&cSorry, you cannot use this.")
		Component noPermission();
		
		@DefaultString("&a...")
		Component ellipses();
		
		@DefaultString("&aReloaded")
		Component reloaded();

		@ConfKey("reload-failed")
		@DefaultString("&cAn error occurred reloading the configuration. Please check the server console.")
		Component reloadFailed();

		@DefaultString("&aRestarted")
		Component restarted();

		@SubSection
		Importing importing();

		interface Importing {

			@ConfKey("in-progress")
			@ConfComments("To prevent mistakes, it is not allowed to import multiple times at once.")
			@DefaultString("&cThere is already an import in progress.")
			Component inProgress();

			@DefaultString("&7Import has started. View your server console for details and progress.")
			Component started();

			@DefaultString("&cUsage: /libertybans import <advancedban|litebans|vanilla|self>")
			Component usage();

			@DefaultString("&7Import completed.")
			Component complete();

			@DefaultString("&cImport failed. View the server console for details.")
			Component failure();

		}

		@SubSection
		Addons addons();

		interface Addons {

			@DefaultString("&cUsage: /libertybans addon <list|reload>")
			Component usage();

			@SubSection
			Listing listing();

			interface Listing {

				@DefaultString("&b&lAddons Installed")
				Component message();

				@DefaultString("&7- %ADDON%")
				ComponentText layout();
			}

			@ConfKey("reload-addon")
			@SubSection
			Reloading reloadAddon();

			interface Reloading {

				@DefaultString("&cUsage: /libertybans addon reload <addon>. To reload all addons, /libertybans reload will suffice.")
				Component usage();

				@ConfKey("does-not-exist")
				@DefaultString("&cThat addon does not exist.")
				Component doesNotExist();

				@DefaultString("&aReloaded addon &e%ADDON%&a.")
				ComponentText success();

				@DefaultString("&cAn error occurred reloading addon configuration. Please check the server console.")
				Component failed();

			}
		}
		
	}
	
	@ConfComments("Specific formatting options")
	@SubSection
	Formatting formatting();
	
	interface Formatting {
		
		@ConfKey("permanent-arguments")
		@ConfComments({
				"There are 2 ways to make permanent punishments. The first is to not specify a time (/ban <player> <reason>).",
				"The second is to specify a permanent amount of time (/ban <player> perm <reason>).",
				"When typing commands, what time arguments will be counted as permanent?"})
		@DefaultStrings({"perm", "permanent", "permanently"})
		Set<String> permanentArguments();
		
		@ConfKey("permanent-display")
		@ConfComments("How should 'permanent' be displayed as a length of time?")
		@SubSection
		PermanentDisplay permanentDisplay();
		
		interface PermanentDisplay {
			
			@ConfComments("What do you call a permanent duration?")
			@DefaultString("Infinite")
			String duration();
			
			@ConfComments("How do you describe the time remaining in a permanent punishment?")
			@DefaultString("Permanent")
			String relative();
			
			@ConfComments("When does a permanent punishment end?")
			@DefaultString("Never")
			String absolute();
			
		}

		@ConfKey("no-time-remaining-display")
		@ConfComments({
				"When there is no more time remaining in a punishment (the punishment has expired),",
				"this becomes the value of the %TIME_REMAINING% variable"})
		@DefaultString("N/A")
		String noTimeRemainingDisplay();
		
		@ConfKey("console-arguments")
		@ConfComments("When using /blame, how should the console be specified?")
		@DefaultStrings("console")
		Set<String> consoleArguments();
		
		@ConfKey("console-display")
		@ConfComments("How should the console be displayed?")
		@DefaultString("Console")
		String consoleDisplay();
		
		@ConfKey("global-scope-display")
		@ConfComments("How should the global scope be displayed?")
		@DefaultString("All servers")
		String globalScopeDisplay();
		
		@ConfKey("punishment-type-display")
		@ConfComments("How should punishment types be displayed?")
		@DefaultMap({
			"ban", "Ban",
			"mute", "Mute",
			"warn", "Warn",
			"kick", "Kick"
		})
		Map<PunishmentType, String> punishmentTypeDisplay();

		@ConfKey("punishment-type-verb-display")
		@ConfComments("How should punishment types be displayed as a verb? Used for the %TYPE_VERB% variable.")
		@DefaultMap({
				"ban", "Banned",
				"mute", "Muted",
				"warn", "Warned",
				"kick", "Kicked"
		})
		Map<PunishmentType, String> punishmentTypeVerbDisplay();

		@ConfKey("punishment-expired-display")
		@ConfComments("How should the %HAS_EXPIRED% variable be displayed?")
		@SubSection
		PunishmentExpiredDisplay punishmentExpiredDisplay();

		interface PunishmentExpiredDisplay {

			@ConfKey("not-expired")
			@ConfComments("How do you describe a punishment which is not expired?")
			@DefaultString("Not expired")
			String notExpired();

			@ConfComments("How do you describe an expired punishment?")
			@DefaultString("Expired")
			String expired();

		}

	}
	
	@SubSection
	AdditionsSection additions();
	
	@SubSection
	RemovalsSection removals();
	
	@SubSection
	ListSection lists();
	
	@SubSection
	Misc misc();

	@SubSection
	AltsSection alts();

	@ConfKey("account-history")
	@SubSection
	AccountHistorySection accountHistory();

	interface Misc {
		
		@ConfKey("unknown-error")
		@DefaultString("&cAn unknown error occurred.")
		Component unknownError();
		
		@ConfKey("sync-chat-denial-message")
		@ConfComments("Only applicable if synchronous enforcement strategy is DENY in the main config")
		@DefaultString("&cSynchronous chat denied. &7Please try again.")
		Component syncDenialMessage();
		
		@SubSection
		@ConfComments("Concerns formatting of relative times and durations")
		Time time();
		
		interface Time {
			
			@DefaultMap({
					"YEARS", "%VALUE% years",
					"MONTHS", "%VALUE% months",
					"WEEKS", "%VALUE% weeks",
					"DAYS", "%VALUE% days",
					"HOURS", "%VALUE% hours",
					"MINUTES", "%VALUE% minutes"})
			Map<ChronoUnit, String> fragments();
			
			@ConfKey("fallback-seconds")
			@ConfComments({
					"Times are formatted to seconds accuracy, but you may not want to display seconds ",
					"for most times. However, for very small durations, you need to display a value in seconds.",
					"If you are using SECONDS in the above section, this value is meaningless."})
			@DefaultString("%VALUE% seconds")
			String fallbackSeconds();
			
			@ConfKey("grammar.comma")
			@ConfComments("If enabled, places commas after each time fragment, except the last one")
			@DefaultBoolean(true)
			boolean useComma();
			
			@ConfKey("grammar.and")
			@ConfComments("What should come before the last fragment? Set to empty text to disable")
			@DefaultString("and ")
			String and();
			
		}
		
	}
	
}
