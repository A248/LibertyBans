/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.env.bungee;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import jakarta.inject.Singleton;

import net.md_5.bungee.api.connection.Connection;

@Singleton
public class StandardAddressReporter implements AddressReporter {

	@Override
	public InetAddress getAddress(Connection bungeePlayer) {
		SocketAddress socketAddress = bungeePlayer.getSocketAddress();
		if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
			return inetSocketAddress.getAddress();
		}
		throw new IllegalStateException("Non-InetSocketAddress addresses are not supported by LibertyBans");
	}
	
}
