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

package space.arim.libertybans.core.commands.extra;

import space.arim.libertybans.core.commands.CommandPackage;
import space.arim.libertybans.core.env.CmdSender;
import space.arim.libertybans.core.punish.permission.PermissionBase;

import java.util.Objects;

public final class NotificationMessage {

	private final CmdSender sender;
	private final PermissionBase permissionBase;
	private boolean silent;

	public NotificationMessage(CmdSender sender, PermissionBase permissionBase) {
		this.sender = Objects.requireNonNull(sender);
		this.permissionBase = Objects.requireNonNull(permissionBase);
	}

	public void evaluate(CommandPackage command) {
		silent = sender.hasPermission(permissionBase.permission("silent")) && command.findHiddenArgument("s");
	}

	public boolean isSilent() {
		return silent;
	}
}
