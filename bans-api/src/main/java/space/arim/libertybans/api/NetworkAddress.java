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
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Lighter alternative to {@code InetAddress} specialised for handling the raw address,
 * and not anything related to hostnames, etc.
 * 
 * @author A248
 *
 */
public final class NetworkAddress {
	
	private final byte[] address;

	private NetworkAddress(byte[] address) {
		this.address = address;
	}
	
	/**
	 * Creates from a {@code InetAddress}
	 * 
	 * @param address the inet address
	 * @return the network address
	 * @throws NullPointerException if {@code address} is null
	 */
	public static NetworkAddress of(InetAddress address) {
		return new NetworkAddress(address.getAddress());
	}
	
	/**
	 * Creates from raw address bytes. For IPv4 addresses, this should be 4 bytes long,
	 * for IPv6, 16 bytes. <br>
	 * <br>
	 * Similar to {@link InetAddress#getByAddress(byte[])}
	 * 
	 * @param address the raw address bytes, in network byte order
	 * @return the network address
	 * @throws IllegalArgumentException if the address bytes array is of illegal length
	 * @throws NullPointerException if {@code address} is null
	 */
	public static NetworkAddress of(byte[] address) {
		if (address.length != 4 && address.length != 16) {
			throw new IllegalArgumentException("Bad address length");
		}
		return new NetworkAddress(address.clone());
	}
	
	/**
	 * Gets the raw address bytes of this network address
	 * 
	 * @return the raw address
	 */
	public byte[] getRawAddress() {
		return address.clone();
	}
	
	/**
	 * Converts this network address to an equivalent {@code InetAddress}
	 * 
	 * @return the inet address
	 */
	public InetAddress toInetAddress() {
		try {
			return InetAddress.getByAddress(address);
		} catch (UnknownHostException ex) {
			throw new AssertionError(ex);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(address);
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof NetworkAddress)) {
			return false;
		}
		NetworkAddress other = (NetworkAddress) object;
		return Arrays.equals(address, other.address);
	}

	/**
	 * Returns a textual representation of this network address
	 * 
	 */
	@Override
	public String toString() {
		return toInetAddress().getHostAddress();
	}
	
}
