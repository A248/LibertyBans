/* 
 * LibertyBans-env-bungee
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-bungee is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-bungee is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-bungee. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.bungee;

import java.net.InetSocketAddress;
import java.util.UUID;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import space.arim.libertybans.core.env.AbstractOnlineTarget;

class BungeeOnlineTarget extends AbstractOnlineTarget {
	
	BungeeOnlineTarget(BungeeEnv env, ProxiedPlayer player) {
		super(env, player);
	}

	@Override
	public boolean hasPermission(String permission) {
		return getRawPlayer().hasPermission(permission);
	}

	@Override
	public UUID getUniqueId() {
		return getRawPlayer().getUniqueId();
	}

	@Override
	public byte[] getAddress() {
		return ((InetSocketAddress) getRawPlayer().getSocketAddress()).getAddress().getAddress();
	}
	
	@Override
	public ProxiedPlayer getRawPlayer() {
		return (ProxiedPlayer) super.getRawPlayer();
	}

}
