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

package space.arim.libertybans.core.punish.permission;

import net.kyori.adventure.text.Component;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.config.PunishmentSection;
import space.arim.libertybans.core.env.CmdSender;

import java.util.Objects;

public record VictimTypeCheck(CmdSender sender, PermissionBase permissionBase) implements VictimPermissionCheck {

	public VictimTypeCheck {
		Objects.requireNonNull(sender);
		Objects.requireNonNull(permissionBase);
	}

	public boolean hasPermission(Victim.VictimType victimType) {
		String typeSuffix = switch (victimType) {
			case PLAYER -> "uuid";
			case ADDRESS -> "ip";
			case COMPOSITE -> "both";
		};
		String permission = permissionBase.permission("target." + typeSuffix);
		return sender.hasPermission(permission);
	}

	@Override
	public boolean checkPermission(Victim victim, PunishmentSection section) {
		Victim.VictimType victimType = victim.getType();
		if (!hasPermission(victimType)) {
			var permissionSection = section.permission();
			Component message = switch (victimType) {
				case PLAYER -> permissionSection.uuid();
				case ADDRESS -> permissionSection.ipAddress();
				case COMPOSITE -> permissionSection.both();
			};
			sender.sendMessage(message);
			return false;
		}
		return true;
	}

}
