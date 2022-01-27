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

package space.arim.libertybans.core.importing;

import java.util.Objects;

/**
 * Represents a unique combination of punishment details. <br>
 * <br>
 * See the wiki for the explanation on AdvancedBan's quirks relating to punishment uniqueness.
 *
 */
final class AdvancedBanUniquePunishmentDetails {

	private final PortablePunishment portablePunishment;

	AdvancedBanUniquePunishmentDetails(PortablePunishment portablePunishment) {
		this.portablePunishment = Objects.requireNonNull(portablePunishment);
	}

	PortablePunishment portablePunishment() {
		return portablePunishment;
	}

	private PortablePunishment.KnownDetails knownDetails() {
		return portablePunishment.knownDetails();
	}

	private PortablePunishment.VictimInfo victimInfo() {
		return portablePunishment.victimInfo();
	}

	private PortablePunishment.OperatorInfo operatorInfo() {
		return portablePunishment.operatorInfo();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AdvancedBanUniquePunishmentDetails that = (AdvancedBanUniquePunishmentDetails) o;
		return knownDetails().equals(that.knownDetails())
				&& victimInfo().equals(that.victimInfo())
				&& operatorInfo().equals(that.operatorInfo());
	}

	@Override
	public int hashCode() {
		int result = knownDetails().hashCode();
		result = 31 * result + victimInfo().hashCode();
		result = 31 * result + operatorInfo().hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "AdvancedBanUniquePunishmentDetails{" +
				"portablePunishment=" + portablePunishment +
				'}';
	}
}
