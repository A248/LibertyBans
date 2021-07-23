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

package space.arim.libertybans.core.env;

import space.arim.libertybans.api.NetworkAddress;

import java.net.InetAddress;
import java.util.Objects;
import java.util.UUID;

public final class UUIDAndAddress {

	private final UUID uuid;
	private final NetworkAddress address;

	public UUIDAndAddress(UUID uuid, NetworkAddress address) {
		this.uuid = Objects.requireNonNull(uuid, "uuid");
		this.address = Objects.requireNonNull(address, "address");
	}

	public UUIDAndAddress(UUID uuid, InetAddress address) {
		this(uuid, NetworkAddress.of(address));
	}

	public UUID uuid() {
		return uuid;
	}

	public NetworkAddress address() {
		return address;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UUIDAndAddress that = (UUIDAndAddress) o;
		return uuid.equals(that.uuid) && address.equals(that.address);
	}

	@Override
	public int hashCode() {
		int result = uuid.hashCode();
		result = 31 * result + address.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "UUIDAndAddress{" +
				"uuid=" + uuid +
				", address=" + address +
				'}';
	}
}
