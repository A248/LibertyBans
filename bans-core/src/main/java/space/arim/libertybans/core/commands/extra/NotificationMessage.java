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

import java.util.Objects;

public final class NotificationMessage {

	private final CmdSender sender;
	private final PunishmentType type;
	private final boolean undo;
	private boolean silent;

	public NotificationMessage(CmdSender sender, PunishmentType type, Mode mode) {
		this.sender = Objects.requireNonNull(sender, "sender");
		this.type = Objects.requireNonNull(type, "type");
		this.undo = mode.equals(Mode.UNDO);
	}

	public void evaluate(CommandPackage command) {
		StringBuilder silentPermission = new StringBuilder();

		silentPermission.append("libertybans.");
		silentPermission.append(type);
		silentPermission.append(".silent");
		if (undo) silentPermission.append("undo");

		silent = sender.hasPermission(silentPermission.toString())
				&& command.findHiddenArgument("s");
	}

	public boolean isSilent() {
		return silent;
	}

	public String notificationPermission() {
		StringBuilder notifyPermission = new StringBuilder();

		notifyPermission.append("libertybans.");
		notifyPermission.append(type);
		notifyPermission.append('.');
		if (undo) notifyPermission.append("un");
		notifyPermission.append("notify");
		if (silent) notifyPermission.append("silent");

		return notifyPermission.toString();
	}

	public enum Mode {
		DO,
		UNDO
	}
}
