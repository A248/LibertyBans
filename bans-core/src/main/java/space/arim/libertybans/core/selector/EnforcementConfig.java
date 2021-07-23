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
package space.arim.libertybans.core.selector;

import java.util.Set;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfDefault.DefaultStrings;

@ConfHeader({"Options related to punishment enforcement and alt account checking",
		"",
		"Alt Account Enforcement and Checking:",
		"There are multiple ways to combat alt accounts in LibertyBans.",
		"First, you can have the plugin automatically detect alt accounts and prevent them from joining, ",
		"with the same ban message. This is controlled by the 'address-strictness' setting.",
		"Second, you can tell your staff members to be on the lookout for alts. They can use ",
		"the /alts command to manually check players they suspect are alt accounts. Also, you can ",
		"use 'enable-alts-auto-show' which will notify staff of players who may be using alts."})
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

	@ConfComments({"If enabled, runs an alt-check on newly joined players, as if by /alts, ",
			"the results of which will be shown to staff members with the libertybans.alts.autoshow permission."})
	@ConfKey("enable-alts-auto-show")
	@ConfDefault.DefaultBoolean(false)
	boolean enableAltsAutoShow();
	
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
		"tell"})
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
	
}
