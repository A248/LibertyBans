/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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

package space.arim.libertybans.core.commands.extra;

import net.kyori.adventure.text.Component;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.config.PunishmentPermissionSection;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.punish.MiscUtil;

import java.util.Objects;

public final class PunishmentPermissionCheck {

	private final CmdSender sender;
	private final PunishmentType type;
	private final Mode mode;

	public PunishmentPermissionCheck(CmdSender sender, PunishmentType type, Mode mode) {
		this.sender = Objects.requireNonNull(sender, "sender");
		this.type = Objects.requireNonNull(type, "type");
		this.mode = Objects.requireNonNull(mode, "mode");
	}

	private String permission(String suffix) {
		return "libertybans." + type + '.' + mode + '.' + suffix;
	}

	private static String permissionForVictim(Victim.VictimType victimType) {
		switch (victimType) {
		case PLAYER:
			return "uuid";
		case ADDRESS:
			return "ip";
		default:
			throw MiscUtil.unknownVictimType(victimType);
		}
	}

	private static Component permissionMessageForVictim(Victim.VictimType victimType, PunishmentPermissionSection permissionSection) {
		switch (victimType) {
		case PLAYER:
			return permissionSection.uuid();
		case ADDRESS:
			return permissionSection.ipAddress();
		default:
			throw MiscUtil.unknownVictimType(victimType);
		}
	}

	public boolean checkPermission(Victim victim, PunishmentPermissionSection permissionSection) {
		Victim.VictimType victimType = victim.getType();
		String permission = permission("target." + permissionForVictim(victimType));
		if (!sender.hasPermission(permission)) {
			sender.sendMessage(permissionMessageForVictim(victimType, permissionSection));
			return false;
		}
		return true;
	}

	boolean canUseSilence() {
		return sender.hasPermission(permission("silent"));
	}

	String notifyPermission(boolean silent) {
		return permission((silent) ? "notifysilent" : "notify");
	}
}
