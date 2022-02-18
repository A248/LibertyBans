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

package space.arim.libertybans.core.selector;

import java.util.Set;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfDefault.DefaultStrings;
import space.arim.dazzleconf.annote.NumericRange;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.core.alts.ConnectionLimitConfig;
import space.arim.libertybans.core.alts.WhichAlts;

@ConfHeader({"Options related to punishment enforcement and alt account checking",
		"",
		"-- Alt Account Enforcement and Checking --",
		"There are multiple ways to combat alt accounts in LibertyBans.",
		"",
		"First, you can have the plugin automatically detect alt accounts and prevent them from joining, ",
		"with the same ban message. This is controlled by the 'address-strictness' setting.",
		"",
		"Second, you can tell your staff members to be on the lookout for alts. They can use ",
		"the /alts command to manually check players they suspect are alt accounts. Also, you can ",
		"use the alts-auto-show feature which will automatically notify staff of players who may be using alts.",
		"",
		"Third, you may use 'composite punishments', a more advanced feature which is described on the wiki."})
public interface EnforcementConfig {

	@ConfKey("address-strictness")
	@ConfComments({"",
		"How strict should IP-based punishments be?",
		"Available options are LENIENT, NORMAL, and STRICT",
		"",
		"LENIENT - If the player's current address matches the punished address, the punishment applies to the player",
		"NORMAL - If any of player's past addresses matches the punished address, the punishment applies to the player",
		"STRICT - If any of player's past addresses match any related address linked by a common player,",
		"the punishment applies to the player"})
	@DefaultString("NORMAL")
	AddressStrictness addressStrictness(); // Sensitive name used in integration testing

	@ConfKey("alts-auto-show")
	@SubSection
	AltsAutoShow altsAutoShow();

	@ConfHeader({
			"Runs an alt-check on newly joined players, as if by /alts, ",
			"the results of which will be shown to staff members with the libertybans.alts.autoshow permission."})
	interface AltsAutoShow {

		@ConfComments("Set to true to enable this feature")
		@ConfDefault.DefaultBoolean(false)
		boolean enable();

		@ConfKey("show-which-alts")
		@ConfComments({
				"Allows determining which alts will be shown by this alt-check",
				"(This does not affect the alts command, which will always show all alts)",
				"ALL_ALTS - shows all alts",
				"BANNED_OR_MUTED_ALTS - shows alts either banned or muted",
				"BANNED_ALTS - shows only banned alts"
		})
		@DefaultString("ALL_ALTS")
		WhichAlts showWhichAlts();

	}

	@ConfKey("connection-limiter")
	@SubSection
	ConnectionLimitConfig connectionLimiter();

	@ConfKey("mute-commands")
	@ConfComments({"",
		"A list of commands muted players will not be able to execute",
		"",
		"This list supports subcommands, which will be enforced if the executed command starts with the list entry.",
		"Additionally, colons in commands, such as\"pluginname:cmd\", cannot be used to bypass this."})
	@DefaultStrings({
		"me",
		"say",
		"msg",
		"reply",
		"r",
		"whisper",
                "w",
		"tell",
                "t"})
	Set<String> muteCommands();
	
	@ConfKey("sync-events-strategy")
	@ConfComments({"",
			"Occasionally, LibertyBans may have to deal with a platform event synchronously",
			"",
			"Currently, this happens rarely, and only on Bukkit, with the enforcement of mutes.",
			"Most of the time, chat events are async. However, if another plugin forces a player",
			"to chat, the event will happen sync. Also, the command event is run sync.",
			"",
			"In these situations, it is possible LibertyBans has no cached mute result and cannot",
			"query the database quickly enough. The following strategies are available:",
			"",
			"* WAIT - wait for the database query to complete. This can block the main thread",
			"* DENY - deny the event, using the 'misc.sync-event-denial' message",
			"* ALLOW - allow the event",
			"",
			"This situation can be avoided by using a proxy, where there is no \"main thread\".",
			"Velocity and BungeeCord are therefore not affected by synchronous events."})
	@DefaultString("ALLOW")
	SyncEnforcement syncEnforcement();

	@ConfKey("alt-account-expiration")
	@SubSection
	AltAccountExpiration altAccountExpiration();

	@ConfHeader({"Controls the expiration of join history as used by manual alt detection.",
			"This allows expiring alt accounts after some time has elapsed.",
			"",
			"This setting does NOT affect enforcement of IP-based punishments.",
			"It applies only to the /alts command and the alts-auto-show feature.",
			"",
			"Note that this feature will not actually delete any data from the database."})
	interface AltAccountExpiration {

		@ConfComments("Whether to enable this feature")
		@ConfDefault.DefaultBoolean(false)
		boolean enable();

		@ConfKey("expiration-time-days")
		@ConfComments("The expiration time, in days.")
		@ConfDefault.DefaultInteger(30)
		@NumericRange(min = 1)
		long expirationTimeDays();

	}
}
