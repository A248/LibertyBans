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

package space.arim.libertybans.core.database.sql;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;

import java.util.Objects;
import java.util.UUID;

public final class DeserializedVictim {

	private final UUID uuid;
	private final NetworkAddress address;

	public DeserializedVictim(UUID uuid, NetworkAddress address) {
		this.uuid = Objects.requireNonNull(uuid, "uuid");
		this.address = Objects.requireNonNull(address, "address");
	}

	public Victim victim(Victim.VictimType victimType) {
		return switch (victimType) {
			case PLAYER -> {
				assert address.equals(EmptyData.ADDRESS) : "Address must be empty for player victims";
				yield PlayerVictim.of(uuid);
			}
			case ADDRESS -> {
				assert uuid.equals(EmptyData.UUID) : "UUID must be empty for address victims";
				yield AddressVictim.of(address);
			}
			case COMPOSITE ->  CompositeVictim.of(uuid, address);
		};
	}

}
