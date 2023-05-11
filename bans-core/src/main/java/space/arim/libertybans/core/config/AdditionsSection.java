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

package space.arim.libertybans.core.config;

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;
import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfDefault.DefaultStrings;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.api.PunishmentType;

@ConfHeader({
		"",
		"Messages regarding /ban, /mute, /warn, /kick",
		"Includes punishment layouts",
		"",
		""})
public interface AdditionsSection {

	interface BanAddition extends PunishmentAdditionSection.WithDurationPerm {

		@Override
		@DefaultString("&cUsage: /ban &e<player> [time] <reason>&c.")
		Component usage();

		@Override
		@DefaultString("&c&o%TARGET%&r&7 cannot be banned.")
		ComponentText exempted();

		@Override
		@DefaultString("&c&o%TARGET%&r&7 is already banned.")
		ComponentText conflicting();

		@Override
		@ConfKey("success.message")
		@DefaultString("&aBanned &c&o%VICTIM%&r&a for &a&o%DURATION%&r&a because of &e&o%REASON%&r&a.")
		ComponentText successMessage();
		
		@Override
		@ConfKey("success.notification")
		@DefaultString("&c&o%OPERATOR%&r&7 banned &c&o%VICTIM%&r&7 for &a&o%DURATION%&r&7 because of &e&o%REASON%&r&7.")
		ComponentText successNotification();
		
		@Override
		@DefaultStrings({
				"&7&lBanned",
				"&cDuration: &e%TIME_REMAINING%",
				"&7",
				"&c&lReason",
				"&7%REASON%",
				"&7",
				"&3&lAppeal Your Punishment",
				"&cWebsite: &7website",
				"&cDiscord: &7discord"})
		ComponentText layout();
		
	}

	interface MuteAddition extends PunishmentAdditionSection.WithDurationPerm {
		
		@Override
		@DefaultString("&cUsage: /mute &e<player> [time] <reason>&c.")
		Component usage();

		@Override
		@DefaultString("&c&o%TARGET%&r&7 cannot be muted.")
		ComponentText exempted();

		@Override
		@DefaultString("&c&o%TARGET%&r&7 is already muted.")
		ComponentText conflicting();

		@Override
		@ConfKey("success.message")
		@DefaultString("&aMuted &c&o%VICTIM%&r&a for &a&o%DURATION%&r&a because of &e&o%REASON%&r&a.")
		ComponentText successMessage();
		
		@Override
		@ConfKey("success.notification")
		@DefaultString("&c&o%OPERATOR%&r&7 muted &c&o%VICTIM%&r&7 for &a&o%DURATION%&r&7 because of &e&o%REASON%&r&7.")
		ComponentText successNotification();
		
		@Override
		@DefaultStrings({
				"&7&lMuted",
				"&cDuration: &e%TIME_REMAINING%",
				"&7",
				"&c&lReason",
				"&7%REASON%"})
		ComponentText layout();
		
	}
	
	interface WarnAddition extends PunishmentAdditionSection.WithDurationPerm {
		
		@Override
		@DefaultString("&cUsage: /warn &e<player> [time] <reason>&c.")
		Component usage();

		@Override
		@DefaultString("&c&o%TARGET%&r&7 cannot be warned.")
		ComponentText exempted();

		@Override
		default ComponentText conflicting() {
			return SHOULD_NOT_CONFLICT;
		}

		@Override
		@ConfKey("success.message")
		@DefaultString("&aWarned &c&o%VICTIM%&r&a for &a&o%DURATION%&r&a because of &e&o%REASON%&r&a.")
		ComponentText successMessage();
		
		@Override
		@ConfKey("success.notification")
		@DefaultString("&c&o%OPERATOR%&r&7 warned &c&o%VICTIM%&r&7 for &a&o%DURATION%&r&7 because of &e&o%REASON%&r&7.")
		ComponentText successNotification();
		
		@Override
		@DefaultStrings({
				"&7&lWarned",
				"&cDuration: &e%TIME_REMAINING%",
				"&7",
				"&c&lReason",
				"&7%REASON%"})
		ComponentText layout();
		
	}

	interface KickAddition extends PunishmentAdditionSection.WithLayout {

		@Override
		@DefaultString("&cUsage: /kick &e<player> <reason>&c.")
		Component usage();

		@Override
		@DefaultString("&c&o%TARGET%&r&7 cannot be kicked.")
		ComponentText exempted();

		@Override
		default ComponentText conflicting() {
			return SHOULD_NOT_CONFLICT;
		}

		@Override
		@ConfKey("success.message")
		@DefaultString("&aKicked &c&o%VICTIM%&r&a because of &e&o%REASON%&r&a.")
		ComponentText successMessage();
		
		@Override
		@ConfKey("success.notification")
		@DefaultString("&c&o%OPERATOR%&r&7 kicked &c&o%VICTIM%&r&7 because of &e&o%REASON%&r&7.")
		ComponentText successNotification();
		
		@Override
		@DefaultStrings({
				"&7&lKicked",
				"&7",
				"&c&lReason",
				"&7%REASON%"})
		ComponentText layout();

		@ConfKey("must-be-online")
		@DefaultString("&c&o%TARGET%&r&7 must be online.")
		ComponentText mustBeOnline();

	}
	
	@SubSection
	BanAddition bans();
	
	@SubSection
	MuteAddition mutes();
	
	@SubSection
	WarnAddition warns();
	
	@SubSection
	KickAddition kicks();

	default PunishmentAdditionSection.WithLayout forType(PunishmentType type) {
		return switch (type) {
			case BAN -> bans();
			case MUTE -> mutes();
			case WARN -> warns();
			case KICK -> kicks();
		};
	}

}
