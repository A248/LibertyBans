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

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import space.arim.libertybans.core.env.OnlineTarget;

class BungeeOnlineTarget implements OnlineTarget {

	private final ProxiedPlayer player;
	
	BungeeOnlineTarget(ProxiedPlayer player) {
		this.player = player;
	}

	@Override
	public boolean hasPermission(String permission) {
		return player.hasPermission(permission);
	}

	@Override
	public UUID getUniqueId() {
		return player.getUniqueId();
	}

	@Override
	public byte[] getAddress() {
		return ((InetSocketAddress) player.getSocketAddress()).getAddress().getAddress();
	}

	@Override
	public void kick(String message) {
		player.disconnect(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));
	}

}
