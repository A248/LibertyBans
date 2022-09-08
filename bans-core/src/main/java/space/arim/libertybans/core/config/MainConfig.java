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

package space.arim.libertybans.core.config;

import space.arim.dazzleconf.annote.ConfComments;
import space.arim.dazzleconf.annote.ConfDefault.DefaultBoolean;
import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfDefault.DefaultStrings;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.core.commands.extra.DurationPermissionsConfig;
import space.arim.libertybans.core.commands.extra.ReasonsConfig;
import space.arim.libertybans.core.commands.extra.TabCompletionConfig;
import space.arim.libertybans.core.selector.EnforcementConfig;
import space.arim.libertybans.core.uuid.UUIDResolutionConfig;

import java.time.ZoneId;
import java.util.List;

@ConfHeader({
		"",
		"",
		"The main LibertyBans configuration",
		"All options here can be updated with /libertybans reload",
		"",
		""})
public interface MainConfig {

	@ConfKey("lang-file")
	@ConfComments({"What language file should be used for messages?",
			"For example, 'en' means LibertyBans will look for a file called 'messages_en.yml'"})
	@DefaultString("en")
	String langFile();
	
	@ConfKey("date-formatting")
	@SubSection
	DateFormatting dateFormatting();
	
	@ConfHeader("Formatting of absolute dates")
	interface DateFormatting {
		
		@ConfKey("format")
		@ConfComments("How should dates be formatted? Follows Java's DateTimeFormatter.ofPattern")
		@DefaultString("dd/MM/yyyy kk:mm")
		DateTimeFormatterWithPattern formatAndPattern();
		
		@ConfKey("timezone")
		@ConfComments({"Do you want to override the timezone? If 'default', the system default timezone is used",
			"The value must be in continent/city format, such as 'America/New_York'. Uses Java's ZoneId.of"})
		@DefaultString("default")
		ZoneId zoneId();
		
	}
	
	@SubSection
	ReasonsConfig reasons();
	
	@SubSection
	DurationPermissionsConfig durationPermissions();
	
	@SubSection
	EnforcementConfig enforcement(); // Sensitive method name
	
	@ConfKey("player-uuid-resolution")
	@SubSection
	UUIDResolutionConfig uuidResolution(); // Sensitive method name
	
	@SubSection
	Commands commands();
	
	interface Commands {

		@ConfKey("enable-tab-completion")
		@ConfComments("Whether to enable tab completion")
		@DefaultBoolean(true)
		boolean tabComplete();

		@ConfKey("tab-completion")
		@SubSection
		TabCompletionConfig tabCompletion();

		@ConfComments({"What commands should be registered as aliases for libertybans commands?",
			"For each command listed here, '/<command>' will be equal to '/libertybans <command>'"})
		@DefaultStrings({
			"ban", "ipban",
		    "mute", "ipmute",
		    "warn", "ipwarn",
		    "kick", "ipkick",
		    "unban", "unbanip",
		    "unmute", "unmuteip",
		    "unwarn", "unwarnip",
		    "banlist",
		    "mutelist",
		    "history",
		    "warns",
		    "blame",
		    "alts",
			"accounthistory"
		})
		List<String> aliases();

		@ConfKey("use-composite-victims-by-default")
		@ConfComments({
				"If this is enabled, it will be as if relevant commands used the -both argument.",
				"Effectively, this makes punishments more strict, since moderators will end up banning UUIDs and addresses"
		})
		@DefaultBoolean(false)
		boolean useCompositeVictimsByDefault();

		@ConfKey("blame-shows-active-only")
		@ConfComments({
				"By default, /blame shows only active punishments made by a staff member.",
				"If you disable this option, /blame will show all punishments, including those revoked and expired.",
				"",
				"If you use the staffrollback addon, you probably want to disable this to make /blame consistent",
				"with the staffrollback operation."
		})
		@DefaultBoolean(true)
		boolean blameShowsActiveOnly();

	}

	@SubSection
	Platforms platforms();

	interface Platforms {

		@SubSection
		Sponge sponge();

		interface Sponge {

			@ConfKey("register-ban-service")
			@ConfComments({
					"Whether to register the ban service for use by other plugins.",
					"If enabled, LibertyBans will replace the server's default banning implementation with its own.",
					"",
					"This option is somewhat technical. For advice on whether you should configure this option,",
					"please ask for support.",
					"It is highly recommended to ask for guidance if you are unsure about this option.",
					"",
					"How this affects compatibility:",
					"- If disabled, the vanilla ban service will remain. Other plugins which query for bans will be",
					"  served with information from vanilla. For example, if a player is banned through LibertyBans",
					"  but not vanilla, then other plugins will think the player is not banned.",
					"- If enabled, uses of the ban service will forward to LibertyBans. Other plugins which query for bans",
					"  will be served with accurate information from LibertyBans.",
					"- However, the LibertyBans ban service has limitations. It cannot issue bans, for example.",
					"  So, if another plugin attempts to create bans using the ban service, you will receive an error.",
					"  If you absolutely need other plugins to issue bans, you must disable this option.",
					"",
					"Note that it is impossible to import vanilla bans if you enable this option. So, if you wish to import",
					"from vanilla, first you need to disable this option. After importing, you may re-enable it."
			})
			@DefaultBoolean(true)
			boolean registerBanService();

		}
	}

}
