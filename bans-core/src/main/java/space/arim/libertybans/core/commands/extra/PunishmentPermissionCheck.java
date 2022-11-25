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

package space.arim.libertybans.core.commands.extra;

import net.kyori.adventure.text.Component;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.config.PunishmentPermissionSection;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.punish.PunishmentPermission;

import java.util.Objects;

public final class PunishmentPermissionCheck {

	private final CmdSender sender;
	private final PunishmentPermission punishmentPermission;

	public PunishmentPermissionCheck(CmdSender sender, PunishmentPermission punishmentPermission) {
		this.sender = Objects.requireNonNull(sender, "sender");
		this.punishmentPermission = Objects.requireNonNull(punishmentPermission, "punishmentPermission");
	}

	private static String permissionForVictimType(Victim.VictimType victimType) {
		return switch (victimType) {
			case PLAYER -> "uuid";
			case ADDRESS -> "ip";
			case COMPOSITE -> "both";
		};
	}

	private static Component permissionMessageForVictimType(Victim.VictimType victimType,
															PunishmentPermissionSection permissionSection) {
		return switch (victimType) {
			case PLAYER -> permissionSection.uuid();
			case ADDRESS -> permissionSection.ipAddress();
			case COMPOSITE -> permissionSection.both();
		};
	}

	public boolean hasPermission(Victim.VictimType victimType) {
		String permission = punishmentPermission.permission("target." + permissionForVictimType(victimType));
		return sender.hasPermission(permission);
	}

	public boolean checkPermission(Victim victim, PunishmentPermissionSection permissionSection) {
		Victim.VictimType victimType = victim.getType();
		if (!hasPermission(victimType)) {
			sender.sendMessage(permissionMessageForVictimType(victimType, permissionSection));
			return false;
		}
		return true;
	}

	boolean canUseSilence() {
		return sender.hasPermission(punishmentPermission.permission("silent"));
	}
}
