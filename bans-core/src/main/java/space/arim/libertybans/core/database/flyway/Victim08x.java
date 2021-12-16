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

package space.arim.libertybans.core.database.flyway;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;
import space.arim.omnibus.util.UUIDUtil;

import java.util.Objects;

public final class Victim08x {

	private final String victimType;
	private final byte[] victim;

	public Victim08x(String victimType, byte[] victim) {
		this.victimType = Objects.requireNonNull(victimType, "type");
		this.victim = Objects.requireNonNull(victim, "victim");
	}

	public Victim deserialize() {
		switch (Victim.VictimType.valueOf(victimType)) {
		case PLAYER:
			return PlayerVictim.of(UUIDUtil.fromByteArray(victim));
		case ADDRESS:
			return AddressVictim.of(victim);
		default:
			throw new IllegalStateException("Illegal 0.8.x victim type: " + victimType);
		}
	}
}
