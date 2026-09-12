/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.ipv6.IPv6Address;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.libertybans.api.NetworkAddress;

import java.util.function.IntPredicate;

final class AddressParser {

	private AddressParser() { }

	static @Nullable NetworkAddress parseIpv4(String targetArg) {
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

	static @Nullable NetworkAddress parseIpv6(String targetArg) {
		if (targetArg.isEmpty()) {
			return null;
		}
		class Check implements IntPredicate {
			private boolean foundColon;

			@Override
			public boolean test(int value) {
				if (value == ':') {
					foundColon = true;
					return true;
				}
				return Character.isDigit(value) || Character.isAlphabetic(value);
			}
		};
		Check check = new Check();
		boolean alphanumericOrColon = targetArg.chars().allMatch(check);
		if (!alphanumericOrColon || !check.foundColon) {
			return null;
		}
		var ipAddress = new IPAddressString(targetArg).getAddress(IPAddress.IPVersion.IPV6);
		if (ipAddress != null && ipAddress.isIPv6()) {
			IPv6Address ipv6Address = ipAddress.toIPv6();
			if (!ipv6Address.hasZone()) {
				byte[] addressBytes = ipv6Address.getBytes();
				if (addressBytes.length == 16) {
					return NetworkAddress.of(addressBytes);
				}
			}
		}
		return null;
	}
}
