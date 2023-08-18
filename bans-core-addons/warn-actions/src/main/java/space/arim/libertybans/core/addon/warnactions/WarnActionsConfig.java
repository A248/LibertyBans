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

package space.arim.libertybans.core.addon.warnactions;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.addon.AddonConfig;
import space.arim.libertybans.core.config.ParsedDuration;
import space.arim.libertybans.core.scope.ConfiguredScope;

import java.time.Duration;
import java.util.Map;

@ConfHeader({
		"This addon allows you to define actions when certain amounts of warns are reached.",
		"The warn tally is per-victim, meaning user punishments and IP punishments are tallied separately.",
		"",
		"Please note this configuration is unordered, meaning it is acceptable to read",
		"auto-commands:",
		"- 8: 'command which runs at eight warns'",
		"- 2: 'command which runs at two warns'",
		"for example."
})
public interface WarnActionsConfig extends AddonConfig {

	@ConfKey("auto-commands")
	@ConfComments({
			"Commands to execute.",
			"",
			"Each command is executed by the console. Note that if LibertyBans is installed",
			"on the proxy, you will not be able to run commands from plugins on the backend servers;",
			"likewise if installed on the backend servers, you will not be able to run proxy commands.",
			"",
			"The available variables are the same as those available in punishment messages.",
			"Do not use a leading slash unless the command is double-slashed (e.g., //wand in WorldEdit)."
	})
	@ConfDefault.DefaultMap({
			"3", "say Lookout, %VICTIM% has three warns",
			"9", "msg %VICTIM% If you receive one more warn, you will die!",
			"10", "kill %VICTIM%"
	})
	Map<Integer, String> autoCommands();

	@ConfKey("auto-punishments")
	@ConfComments({
			"Punishments to perform.",
			"",
			"Each punishment is performed as if by the console. The setting broadcast-notification controls whether",
			"punishment notifications will be broadcast as usual; if false, no notifications are sent."
	})
	@ConfDefault.DefaultObject("autoPunishmentsDefaults")
	Map<Integer, @SubSection WarnActionPunishment> autoPunishments();

	static Map<Integer, WarnActionPunishment> autoPunishmentsDefaults() {
		return Map.of(
				4, new WarnActionPunishment() {
					@Override
					public PunishmentType type() {
						return PunishmentType.MUTE;
					}

					@Override
					public String reason() {
						return "You reached 4 warnings";
					}

					@Override
					public ParsedDuration duration() {
						return new ParsedDuration("4h", Duration.ofHours(4L));
					}

					@Override
					public ConfiguredScope scope() {
						return ConfiguredScope.defaultPunishingScope();
					}

					@Override
					public boolean broadcastNotification() {
						return false;
					}
				},
				12, new WarnActionPunishment() {
					@Override
					public PunishmentType type() {
						return PunishmentType.BAN;
					}

					@Override
					public String reason() {
						return "You must be banned if you truly managed to reach 12 warnings.";
					}

					@Override
					public ParsedDuration duration() {
						return new ParsedDuration("perm", Duration.ZERO);
					}

					@Override
					public ConfiguredScope scope() {
						return ConfiguredScope.defaultPunishingScope();
					}

					@Override
					public boolean broadcastNotification() {
						return true;
					}
				}
		);
	}

	interface WarnActionPunishment {

		PunishmentType type();

		String reason();

		ParsedDuration duration();

		ConfiguredScope scope();

		@ConfKey("broadcast-notification")
		boolean broadcastNotification();

	}
}
