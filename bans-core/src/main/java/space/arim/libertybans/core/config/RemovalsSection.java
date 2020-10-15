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

import space.arim.api.chat.SendableMessage;
import space.arim.api.chat.manipulator.SendableMessageManipulator;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.punish.MiscUtil;

import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;

@ConfHeader("Regards /unban, /unmute, /unwarn")
public interface RemovalsSection {

	interface PunishmentRemoval {
		
		SendableMessage usage();
		
		SendableMessage permissionCommand();
		
		SendableMessageManipulator notFound();
		
		SendableMessageManipulator successMessage();
		
		SendableMessageManipulator successNotification();
		
	}
	
	interface BanRemoval extends PunishmentRemoval {
		
		@Override
		@DefaultString("&cUsage: /unban &e<player>&c.")
		SendableMessage usage();
		
		@Override
		@ConfKey("permission.command")
		@DefaultString("&cYou may not unban other players.")
		SendableMessage permissionCommand();
		
		@Override
		@ConfKey("not-found")
		@DefaultString("&c&o%TARGET%&r&7 is not banned.")
		SendableMessageManipulator notFound();
		
		@Override
		@ConfKey("success.message")
		@DefaultString("&7Unbanned &c&o%VICTIM%&r&7.")
		SendableMessageManipulator successMessage();
		
		@Override
		@ConfKey("success.notification")
		@DefaultString("&c&o%UNOPERATOR%&r&7 unbanned &c&o%TARGET%&r&7.")
		SendableMessageManipulator successNotification();
		
	}
	
	interface MuteRemoval extends PunishmentRemoval {
		
		@Override
		@DefaultString("&cUsage: /unmute &e<player>&c.")
		SendableMessage usage();
		
		@Override
		@ConfKey("permission.command")
		@DefaultString("&cYou may not unmute other players.")
		SendableMessage permissionCommand();
		
		@Override
		@ConfKey("not-found")
		@DefaultString("&c&o%TARGET%&r&7 is not muted.")
		SendableMessageManipulator notFound();
		
		@Override
		@ConfKey("success.message")
		@DefaultString("&7Unmuted &c&o%VICTIM%&r&7.")
		SendableMessageManipulator successMessage();
		
		@Override
		@ConfKey("success.notification")
		@DefaultString("&c&o%UNOPERATOR%&r&7 unmuted &c&o%TARGET%&r&7.")
		SendableMessageManipulator successNotification();
		
	}
	
	interface WarnRemoval extends PunishmentRemoval {
		
		@Override
		@DefaultString("&cUsage: /unwarn &e<player> <id>&c.")
		SendableMessage usage();
		
		@Override
		@ConfKey("permission.command")
		@DefaultString("&cYou may not unwarn other players.")
		SendableMessage permissionCommand();
		
		@Override
		@ConfKey("not-found")
		@DefaultString("&c&o%TARGET%&r&7 does not have a warn by &c&o%ID%&r&7.")
		SendableMessageManipulator notFound();
		
		@ConfKey("not-a-number")
		@DefaultString("&c&o%ID_ARG%&r&7 is not a number.")
		SendableMessageManipulator notANumber();
		
		@Override
		@ConfKey("success.message")
		@DefaultString("&7Unmuted &c&o%VICTIM%&r&7.")
		SendableMessageManipulator successMessage();
		
		@Override
		@ConfKey("success.notification")
		@DefaultString("&c&o%UNOPERATOR%&r&7 unmuted &c&o%VICTIM%&r&7.")
		SendableMessageManipulator successNotification();
		
	}
	
	@SubSection
	BanRemoval bans();
	
	@SubSection
	MuteRemoval mutes();
	
	@SubSection
	WarnRemoval warns();

	default PunishmentRemoval forType(PunishmentType type) {
		switch (type) {
		case BAN:
			return bans();
		case MUTE:
			return mutes();
		case WARN:
			return warns();
		case KICK:
			throw new IllegalArgumentException("Cannot undo kicks");
		default:
			throw MiscUtil.unknownType(type);
		}
	}
	
}
