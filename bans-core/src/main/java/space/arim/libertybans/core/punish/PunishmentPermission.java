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

package space.arim.libertybans.core.punish;

import space.arim.libertybans.api.PunishmentType;

import java.util.Objects;

public final class PunishmentPermission {

	private final PunishmentType type;
	private final Mode mode;

	public PunishmentPermission(PunishmentType type, Mode mode) {
		this.type = Objects.requireNonNull(type, "type");
		this.mode = Objects.requireNonNull(mode, "mode");
	}

	public String permission(String suffix) {
		return "libertybans." + type + '.' + mode + '.' + suffix;
	}

	public String notifyPermission(boolean silent) {
		return permission((silent) ? "notifysilent" : "notify");
	}

	@Override
	public String toString() {
		return "PunishmentPermission{" +
				"type=" + type +
				", mode=" + mode +
				'}';
	}
}
