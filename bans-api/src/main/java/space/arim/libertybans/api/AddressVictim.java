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

/**
 * An IP address as the victim of a punishment
 * 
 * @author A248
 *
 */
public class AddressVictim extends Victim {

	private final InetAddress address;
	
	private AddressVictim(InetAddress address) {
		super(VictimType.ADDRESS);
		this.address = address;
	}
	
	/**
	 * Gets a victim for the specified address
	 * 
	 * @param address the address
	 * @return a victim representing the address
	 */
	public static AddressVictim of(InetAddress address) {
		return of(address);
	}
	
	/**
	 * Gets the IP address represented by this victim
	 * 
	 * @return the IP address this victim represents
	 */
	public InetAddress getAddress() {
		return address;
	}

}
