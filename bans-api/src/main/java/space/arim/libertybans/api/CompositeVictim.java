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

package space.arim.libertybans.api;

import java.net.InetAddress;
import java.util.Objects;
import java.util.UUID;

/**
 * An victim which is a composite of a UUID and an IP address.
 * This can be thought of as an IP ban which is slightly more powerful,
 * because it applies to the banned user's UUID as well.
 *
 * @author A248
 *
 */
public final class CompositeVictim extends Victim {

	private final UUID uuid;
	private final NetworkAddress address;

	private CompositeVictim(UUID uuid, NetworkAddress address) {
		this.uuid = Objects.requireNonNull(uuid, "uuid");
		this.address = Objects.requireNonNull(address, "address");
	}

	/**
	 * Gets a victim for the UUID and address
	 *
	 * @param uuid the UUID
	 * @param address the network address
	 * @return a composite victim
	 */
	public static CompositeVictim of(UUID uuid, NetworkAddress address) {
		return new CompositeVictim(uuid, address);
	}

	/**
	 * Gets a victim for the UUID and address
	 *
	 * @param uuid the UUID
	 * @param address the inet address
	 * @return a composite victim
	 */
	public static CompositeVictim of(UUID uuid, InetAddress address) {
		return new CompositeVictim(uuid, NetworkAddress.of(address));
	}

	/**
	 * Gets a victim for the UUID and address bytes. Shortcut for
	 * <code> CompositeVictim.of(uuid, NetworkAddress.of(address)) </code>
	 *
	 * @param uuid the UUID
	 * @param address the raw address bytes
	 * @return a composite victim
	 * @throws IllegalArgumentException if the address bytes array is of illegal length
	 */
	public static CompositeVictim of(UUID uuid, byte[] address) {
		return new CompositeVictim(uuid, NetworkAddress.of(address));
	}

	/**
	 * Gets this victim's type: {@link VictimType#COMPOSITE}
	 *
	 */
	@Override
	public VictimType getType() {
		return VictimType.COMPOSITE;
	}

	/**
	 * Gets the UUID of this victim
	 *
	 * @return the player UUID
	 */
	public UUID getUUID() {
		return uuid;
	}

	/**
	 * Gets the network address of this victim
	 *
	 * @return the address
	 */
	public NetworkAddress getAddress() {
		return address;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CompositeVictim that = (CompositeVictim) o;
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
		return "CompositeVictim{" +
				"uuid=" + uuid +
				", address=" + address +
				'}';
	}
}
