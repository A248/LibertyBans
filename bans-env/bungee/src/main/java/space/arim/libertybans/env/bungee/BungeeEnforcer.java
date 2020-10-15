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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.function.Consumer;

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.annote.PlatformPlayer;

import space.arim.libertybans.core.env.AbstractEnvEnforcer;
import space.arim.libertybans.core.env.TargetMatcher;

import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;

class BungeeEnforcer extends AbstractEnvEnforcer {

	BungeeEnforcer(BungeeEnv env) {
		super(env.core, env);
	}
	
	@Override
	protected BungeeEnv env() {
		return (BungeeEnv) super.env();
	}
	
	@Override
	protected void sendToThoseWithPermission0(String permission, SendableMessage message) {
		for (ProxiedPlayer player : env().getPlugin().getProxy().getPlayers()) {
			if (player.hasPermission(permission)) {
				env().getPlatformHandle().sendMessage(player, message);
			}
		}
	}
	
	@Override
	public void doForPlayerIfOnline(UUID uuid, Consumer<@PlatformPlayer Object> callback) {
		ProxiedPlayer player = env().getPlugin().getProxy().getPlayer(uuid);
		if (player != null) {
			callback.accept(player);
		}
	}
	
	@Override
	public void enforceMatcher(TargetMatcher matcher) {
		for (ProxiedPlayer player : env().getPlugin().getProxy().getPlayers()) {
			if (matcher.matches(player.getUniqueId(), getAddress(player))) {
				matcher.callback().accept(player);
			}
		}
	}
	
	InetAddress getAddress(Connection bungeePlayer) {
		SocketAddress socketAddress = bungeePlayer.getSocketAddress();
		if (socketAddress instanceof InetSocketAddress) {
			return ((InetSocketAddress) socketAddress).getAddress();
		}
		throw new IllegalStateException("Non-InetSocketAddress addresses are not supported by LibertyBans");
	}
	
	@Override
	public UUID getUniqueIdFor(@PlatformPlayer Object player) {
		return ((ProxiedPlayer) player).getUniqueId();
	}
	
	@Override
	public InetAddress getAddressFor(@PlatformPlayer Object player) {
		return getAddress((ProxiedPlayer) player);
	}
	
}
