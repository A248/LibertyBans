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

package space.arim.libertybans.core.commands.extra;

import space.arim.libertybans.api.NetworkAddress;

final class AddressParser {

	private AddressParser() { }

	static NetworkAddress parseIpv4(String targetArg) {
		String[] octetStrings = targetArg.split("\\.");
		if (octetStrings.length != 4) {
			return null;
		}
		byte[] ipv4 = new byte[4];
		for (int n = 0; n < 4; n++) {
			String octetString = octetStrings[n];
			int octet;
			try {
				octet = Integer.parseUnsignedInt(octetString);
			} catch (NumberFormatException ex) {
				return null;
			}
			if (octet < 0 || octet > 255) {
				return null;
			}
			ipv4[n] = (byte) octet;
		}
		return NetworkAddress.of(ipv4);
	}
}
