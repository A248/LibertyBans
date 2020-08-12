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

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.TargetMatcher;

import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;

class BungeeEnforcer implements EnvEnforcer {

	private final BungeeEnv env;
	
	BungeeEnforcer(BungeeEnv env) {
		this.env = env;
	}
	
	@Override
	public void sendToThoseWithPermission(String permission, SendableMessage message) {
		for (ProxiedPlayer player : env.getPlugin().getProxy().getPlayers()) {
			if (player.hasPermission(permission)) {
				env.handle.sendMessage(player, message);
			}
		}
	}
	
	@Override
	public void kickByUUID(UUID uuid, SendableMessage message) {
		ProxiedPlayer player = env.getPlugin().getProxy().getPlayer(uuid);
		if (player != null) {
			env.handle.disconnectUser(player, message);
		}
	}
	
	/*@Override
	public CentralisedFuture<Set<OnlineTarget>> getOnlineTargets() {
		Set<OnlineTarget> result = new HashSet<>();
		for (ProxiedPlayer player : env.getPlugin().getProxy().getPlayers()) {
			result.add(new BungeeOnlineTarget(env, player));
		}
		return env.core.getFuturesFactory().completedFuture(result);
	}*/
	
	private void enforce(ProxiedPlayer player, TargetMatcher matcher) {
		if (matcher.kick()) {
			env.handle.disconnectUser(player, matcher.message());
		} else {
			env.handle.sendMessage(player, matcher.message());
		}
	}
	
	@Override
	public void enforceMatcher(TargetMatcher matcher) {
		for (ProxiedPlayer player : env.getPlugin().getProxy().getPlayers()) {
			if (matcher.uuids().contains(player.getUniqueId()) || matcher.addresses().contains(getAddress(player))) {
				enforce(player, matcher);
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
	
}
