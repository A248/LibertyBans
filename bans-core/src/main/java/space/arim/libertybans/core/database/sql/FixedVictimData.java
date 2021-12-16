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

package space.arim.libertybans.core.database.sql;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Victim;

import java.util.Objects;
import java.util.UUID;

public final class FixedVictimData implements VictimData {

	private final Victim.VictimType type;
	private final UUID uuid;
	private final NetworkAddress address;

	public FixedVictimData(Victim.VictimType type, UUID uuid, NetworkAddress address) {
		this.type = Objects.requireNonNull(type, "type");
		this.uuid = Objects.requireNonNull(uuid, "uuid");
		this.address = Objects.requireNonNull(address, "address");
	}

	public static FixedVictimData from(VictimData delegate) {
		return new FixedVictimData(delegate.type(), delegate.uuid(), delegate.address());
	}

	@Override
	public Victim.VictimType type() {
		return type;
	}

	@Override
	public UUID uuid() {
		return uuid;
	}

	@Override
	public NetworkAddress address() {
		return address;
	}

	@Override
	public String toString() {
		return "FixedVictimData{" +
				"uuid=" + uuid +
				", address=" + address +
				'}';
	}
}
