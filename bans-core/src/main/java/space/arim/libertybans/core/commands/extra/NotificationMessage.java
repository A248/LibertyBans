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

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.punish.Mode;
import space.arim.libertybans.core.punish.PunishmentPermission;

import java.util.Objects;

public final class NotificationMessage {

	private final PunishmentPermissionCheck permissionCheck;
	private boolean silent;

	public NotificationMessage(PunishmentPermissionCheck permissionCheck) {
		this.permissionCheck = Objects.requireNonNull(permissionCheck, "permissionCheck");
	}

	public NotificationMessage(CmdSender sender, PunishmentType type, Mode mode) {
		this(new PunishmentPermissionCheck(sender, new PunishmentPermission(type, mode)));
	}

	public void evaluate(CommandPackage command) {
		silent = permissionCheck.canUseSilence() && command.findHiddenArgument("s");
	}

	public boolean isSilent() {
		return silent;
	}
}
