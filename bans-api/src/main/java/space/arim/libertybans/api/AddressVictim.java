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
 * An IP address as the victim of a punishment
 * 
 * @author A248
 *
 */
public class AddressVictim extends Victim {

	private final NetworkAddress address;
	
	public AddressVictim(NetworkAddress address) {
		super(VictimType.ADDRESS);
		this.address = address;
	}
	
	/**
	 * Gets a victim for the specified address
	 * 
	 * @param address the network address
	 * @return a victim representing the address
	 */
	public static AddressVictim of(NetworkAddress address) {
		return of(address);
	}
	
	/**
	 * Gets the network address represented by this victim
	 * 
	 * @return the network address this victim represents
	 */
	public NetworkAddress getAddress() {
		return address;
	}
	
	/**
	 * Lighter alternative to {@code InetAddress} specialised for handling the raw address,
	 * and not anything related to hostnames &c.
	 * 
	 * @author A248
	 *
	 */
	public static final class NetworkAddress {
		
		private final byte[] address;
		
		/**
		 * Creates from an address byte array. For IPv4 addresses, this should be 4 bytes long,
		 * for IPv6, 16 bytes. <br>
		 * <br>
		 * The array follows the same specifications as {@link InetAddress#getAddress()}
		 * 
		 * @param address the address array
		 * @throws IllegalArgumentException
		 */
		public NetworkAddress(byte[] address) {
			if (address.length != 4 && address.length != 16) {
				throw new IllegalArgumentException("Bad address length");
			}
			this.address = address;
		}
		
		/**
		 * Creates from a {@code InetAddress}
		 * 
		 * @param address the inet address
		 */
		public NetworkAddress(InetAddress address) {
			// Note: Should never throw IllegalArgumentException
			this(address.getAddress());
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

}
