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

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.punish.MiscUtil;

import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfDefault.DefaultStrings;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;

@ConfHeader({
		"",
		"Messages regarding /ban, /mute, /warn, /kick",
		"Includes punishment layouts",
		"",
		""})
public interface AdditionsSection {

	interface PunishmentAddition {
		
		Component usage();
		
		Component permissionCommand();
		
		Component permissionIpAddress();
		
		ComponentText successMessage();
		
		ComponentText successNotification();
		
		ComponentText layout();
		
	}
	
	interface PunishmentAdditionWithDurationPerm extends PunishmentAddition {
		
		ComponentText permissionDuration();
		
	}
	
	interface ExclusivePunishmentAddition extends PunishmentAdditionWithDurationPerm {
		
		ComponentText conflicting();
		
	}
	
	interface BanAddition extends ExclusivePunishmentAddition {
		
		@Override
		@DefaultString("&cUsage: /ban &e<player> [time] <reason>&c.")
		Component usage();
		
		@Override
		@ConfKey("permission.command")
		@DefaultString("&cYou may not ban other players.")
		Component permissionCommand();
		
		@Override
		@ConfKey("permission.ip-address")
		@DefaultString("&cYou may not ban by IP address.")
		Component permissionIpAddress();
		
		@Override
		@ConfKey("permission.duration")
		@DefaultString("&cYou may not ban other players for &e%DURATION%&c.")
		ComponentText permissionDuration();

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
	
	interface MuteAddition extends ExclusivePunishmentAddition {
		
		@Override
		@DefaultString("&cUsage: /mute &e<player> [time] <reason>&c.")
		Component usage();
		
		@Override
		@ConfKey("permission.command")
		@DefaultString("&cYou may not mute other players.")
		Component permissionCommand();
		
		@Override
		@ConfKey("permission.ip-address")
		@DefaultString("&cYou may not mute by IP address.")
		Component permissionIpAddress();
		
		@Override
		@ConfKey("permission.duration")
		@DefaultString("&cYou may not mute other players for &e%DURATION%&c.")
		ComponentText permissionDuration();

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
	
	interface WarnAddition extends PunishmentAdditionWithDurationPerm {
		
		@Override
		@DefaultString("&cUsage: /warn &e<player> [time] <reason>&c.")
		Component usage();
		
		@Override
		@ConfKey("permission.command")
		@DefaultString("&cYou may not warn other players.")
		Component permissionCommand();
		
		@Override
		@ConfKey("permission.ip-address")
		@DefaultString("&cYou may not warn by IP address.")
		Component permissionIpAddress();
		
		@Override
		@ConfKey("permission.duration")
		@DefaultString("&cYou may not warn other players for &e%DURATION%&c.")
		ComponentText permissionDuration();
		
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
	
	interface KickAddition extends PunishmentAddition {
		
		@Override
		@DefaultString("&cUsage: /kick &e<player> <reason>&c.")
		Component usage();
		
		@Override
		@ConfKey("permission.command")
		@DefaultString("&cYou may not kick other players.")
		Component permissionCommand();
		
		@Override
		@ConfKey("permission.ip-address")
		@DefaultString("&cYou may not kick by IP address.")
		Component permissionIpAddress();
		
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
		
	}
	
	@SubSection
	BanAddition bans();
	
	@SubSection
	MuteAddition mutes();
	
	@SubSection
	WarnAddition warns();
	
	@SubSection
	KickAddition kicks();
	
	default PunishmentAddition forType(PunishmentType type) {
		switch (type) {
		case BAN:
			return bans();
		case MUTE:
			return mutes();
		case WARN:
			return warns();
		case KICK:
			return kicks();
		default:
			throw MiscUtil.unknownType(type);
		}
	}
	
}
