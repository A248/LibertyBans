/*
 * LibertyBans
 * Copyright © 2024 Anand Beh
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

import java.util.List;
import java.util.Set;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfDefault.DefaultStrings;
import space.arim.dazzleconf.annote.NumericRange;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.api.select.AddressStrictness;
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
		"Available options are LENIENT, NORMAL, STERN, and STRICT",
		"",
		"An IP-based punishment applies to a player if:",
		"LENIENT - The player's current address matches the punished address",
		"NORMAL - Any of the player's past addresses matches the punished address",
		"STERN - Any of the player's past addresses matches any of the past addresses of the punished user",
		"",
		"STRICT is the same as STERN, but also enforces user punishments as stringently as IP-based punishments.",
		"    (Using STRICT turns all user punishments into IP-based punishments)"
	})
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
			"t",
			"clan chat"
	})
	Set<String> muteCommands();

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

	@ConfKey("alts-registry")
	@SubSection
	AltsRegistry altsRegistry();

	@ConfHeader({"Controls if all servers should register the IP address of the player connecting."})
	interface AltsRegistry {

		@ConfComments({"The server names in this list will be excluded from associating the IP address of the player connecting.",
				"Please note that the server's name in the list should be the same as the ones in your proxy configuration.",
				"This is intended to be used by LibertyBans proxy installations.",
				"If you are planning to use this feature, you MUST enable the 'enforce-server-switch' option."
				})
		@ConfKey("servers-without-ip-registration")
		@DefaultStrings({"auth"})
		List<String> serversWithoutRegistration();

		@ConfComments({"If you want to register the IP address of the player connecting, set this to true.",
				"If you are running a proxy and don't want to register the IP when players connect, ",
				"set this to false and add the authentication servers' names in the list above.",
				"If this is a backend server, set it to false; if it's an authentication server, set to true."})
		@ConfKey("should-register-on-connection)")
		@ConfDefault.DefaultBoolean(true)
		boolean shouldRegisterOnConnection();
	}
}
