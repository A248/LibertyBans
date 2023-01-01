/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static space.arim.libertybans.core.env.Interlocutor.IP_ADDRESS_PATTERN;

public class InterlocutorTest {

	private boolean matches(String address) {
		return IP_ADDRESS_PATTERN.matcher(address).matches();
	}

	@Test
	public void ipv4Address() {
		assertTrue(matches("127.0.0.1"));
		assertTrue(matches("192.168.1.1"));
		assertTrue(matches("192.168.1.255"));
		assertTrue(matches("255.255.255.255"));
		assertTrue(matches("0.0.0.0"));
	}

	@Test
	public void ipv6Address() {
		assertTrue(matches("fffe:3465:efab:23fe:2235:6565:aaab:0001"));
	}

	@Test
	public void notAnIpAddress() {
		assertFalse(matches("not an IP address"));
		assertFalse(matches("4.4"));
		assertFalse(matches("125.58"));
	}

	@Test
	public void replacement() {
		assertEquals(
				"IPv4 address: <censored>.",
				IP_ADDRESS_PATTERN.matcher("IPv4 address: 192.168.1.255.").replaceAll("<censored>")
		);
		assertEquals(
				"IPv6 address: <censored>.",
				IP_ADDRESS_PATTERN.matcher("IPv6 address: fffe:3465:efab:23fe:2235:6565:aaab:0001.").replaceAll("<censored>")
		);
		assertEquals(
				"IP addresses: <censored> and <censored>.",
				IP_ADDRESS_PATTERN.matcher("IP addresses: 192.168.1.255 and fffe:3465:efab:23fe:2235:6565:aaab:0001.").replaceAll("<censored>")
		);
	}

}
