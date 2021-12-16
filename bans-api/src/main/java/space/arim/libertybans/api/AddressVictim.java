/* 
 * LibertyBans-api
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-api. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.api;

import java.net.InetAddress;
import java.util.Objects;

/**
 * An IP address as the victim of a punishment
 * 
 * @author A248
 *
 */
public final class AddressVictim extends Victim {

	private final NetworkAddress address;
	
	private AddressVictim(NetworkAddress address) {
		this.address = Objects.requireNonNull(address, "address");
	}
	
	/**
	 * Gets a victim for the address
	 * 
	 * @param address the network address
	 * @return an address victim
	 */
	public static AddressVictim of(NetworkAddress address) {
		return new AddressVictim(address);
	}
	
	/**
	 * Gets a victim for the address
	 * 
	 * @param address the inet address
	 * @return an address victim
	 */
	public static AddressVictim of(InetAddress address) {
		return new AddressVictim(NetworkAddress.of(address));
	}
	
	/**
	 * Gets a victim for the specified address bytes. Shortcut for
	 * <code> AddressVictim.of(NetworkAddress.of(address)) </code>
	 * 
	 * @param address the raw address bytes
	 * @return an address victim
	 * @throws IllegalArgumentException if the address bytes array is of illegal length
	 */
	public static AddressVictim of(byte[] address) {
		return new AddressVictim(NetworkAddress.of(address));
	}
	
	/**
	 * Gets this victim's type: {@link VictimType#ADDRESS}
	 * 
	 */
	@Override
	public VictimType getType() {
		return VictimType.ADDRESS;
	}

	/**
	 * Gets the network address of this victim
	 * 
	 * @return the network address
	 */
	public NetworkAddress getAddress() {
		return address;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AddressVictim that = (AddressVictim) o;
		return address.equals(that.address);
	}

	@Override
	public int hashCode() {
		return address.hashCode();
	}

	@Override
	public String toString() {
		return "AddressVictim{" +
				"address=" + address +
				'}';
	}
}
