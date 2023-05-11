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
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.api.PunishmentType;

@ConfHeader("Regards /unban, /unmute, /unwarn")
public interface RemovalsSection {

	interface PunishmentRemoval extends PunishmentSection {

		Component usage();

		@SubSection
		@Override
		VictimPermissionSection permission();

		ComponentText notFound();
		
		ComponentText successMessage();
		
		ComponentText successNotification();
		
	}
	
	interface BanRemoval extends PunishmentRemoval {
		
		@Override
		@DefaultString("&cUsage: /unban &e<player>&c.")
		Component usage();

		@Override
		@ConfKey("not-found")
		@DefaultString("&c&o%TARGET%&r&7 is not banned.")
		ComponentText notFound();
		
		@Override
		@ConfKey("success.message")
		@DefaultString("&7Unbanned &c&o%VICTIM%&r&7.")
		ComponentText successMessage();
		
		@Override
		@ConfKey("success.notification")
		@DefaultString("&c&o%UNOPERATOR%&r&7 unbanned &c&o%VICTIM%&r&7.")
		ComponentText successNotification();
		
	}
	
	interface MuteRemoval extends PunishmentRemoval {
		
		@Override
		@DefaultString("&cUsage: /unmute &e<player>&c.")
		Component usage();

		@Override
		@ConfKey("not-found")
		@DefaultString("&c&o%TARGET%&r&7 is not muted.")
		ComponentText notFound();
		
		@Override
		@ConfKey("success.message")
		@DefaultString("&7Unmuted &c&o%VICTIM%&r&7.")
		ComponentText successMessage();
		
		@Override
		@ConfKey("success.notification")
		@DefaultString("&c&o%UNOPERATOR%&r&7 unmuted &c&o%VICTIM%&r&7.")
		ComponentText successNotification();
		
	}
	
	interface WarnRemoval extends PunishmentRemoval {
		
		@Override
		@DefaultString("&cUsage: /unwarn &e<player> <id>&c.")
		Component usage();

		@Override
		@ConfKey("not-found")
		@DefaultString("&c&o%TARGET%&r&7 does not have a warn by &c&o%ID%&r&7.")
		ComponentText notFound();
		
		@ConfKey("not-a-number")
		@DefaultString("&c&o%ID_ARG%&r&7 is not a number.")
		ComponentText notANumber();
		
		@Override
		@ConfKey("success.message")
		@DefaultString("&7Unwarned &c&o%VICTIM%&r&7.")
		ComponentText successMessage();
		
		@Override
		@ConfKey("success.notification")
		@DefaultString("&c&o%UNOPERATOR%&r&7 unwarned &c&o%VICTIM%&r&7.")
		ComponentText successNotification();
		
	}
	
	@SubSection
	BanRemoval bans();
	
	@SubSection
	MuteRemoval mutes();
	
	@SubSection
	WarnRemoval warns();

	default PunishmentRemoval forType(PunishmentType type) {
		return switch (type) {
			case BAN -> bans();
			case MUTE -> mutes();
			case WARN -> warns();
			case KICK -> throw new IllegalArgumentException("Cannot undo kicks");
		};
	}

}
