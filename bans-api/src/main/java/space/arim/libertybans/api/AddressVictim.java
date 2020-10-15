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

import space.arim.libertybans.api.Victim.VictimType;

/**
 * An IP address as the victim of a punishment
 * 
 * @author A248
 *
 */
public final class AddressVictim extends Victim {

	private final NetworkAddress address;
	
	private AddressVictim(NetworkAddress address) {
		this.address = address;
	}
	
	/**
	 * Gets a victim for the specified {@link NetworkAddress}
	 * 
	 * @param address the network address
	 * @return a victim representing the address
	 * @throws NullPointerException if {@code address} is null
	 */
	public static AddressVictim of(NetworkAddress address) {
		return new AddressVictim(Objects.requireNonNull(address, "address"));
	}
	
	/**
	 * Gets a victim for the specified {@link InetAddress}
	 * 
	 * @param address the inet address
	 * @return a victim representing the address
	 */
	public static AddressVictim of(InetAddress address) {
		return new AddressVictim(NetworkAddress.of(address));
	}
	
	/**
	 * Gets a victim for the specified address bytes. Shortcut for
	 * <code> AddressVictim.of(NetworkAddress.of(address)) </code>
	 * 
	 * @param address the raw address bytes
	 * @return a victim representing the address
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
	 * Gets the network address represented by this victim
	 * 
	 * @return the network address this victim represents
	 */
	public NetworkAddress getAddress() {
		return address;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + address.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof AddressVictim)) {
			return false;
		}
		AddressVictim other = (AddressVictim) object;
		return address.equals(other.address);
	}

	@Override
	public String toString() {
		return "AddressVictim [address=" + address + "]";
	}

}
