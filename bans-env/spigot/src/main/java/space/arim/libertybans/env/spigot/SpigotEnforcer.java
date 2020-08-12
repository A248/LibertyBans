/* 
 * LibertyBans-env-spigot
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-spigot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-spigot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-spigot. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.spigot;

import java.util.UUID;

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.TargetMatcher;

import org.bukkit.entity.Player;

class SpigotEnforcer implements EnvEnforcer {

	private final SpigotEnv env;
	
	SpigotEnforcer(SpigotEnv env) {
		this.env = env;
	}
	
	@Override
	public void sendToThoseWithPermission(String permission, SendableMessage message) {
		for (Player player : env.getPlugin().getServer().getOnlinePlayers()) {
			if (player.hasPermission(permission)) {
				env.handle.sendMessage(player, message);
			}
		}
	}

	@Override
	public void kickByUUID(UUID uuid, SendableMessage message) {
		env.core.getFuturesFactory().runSync(() -> {
			Player player = env.getPlugin().getServer().getPlayer(uuid);
			if (player != null) {
				env.handle.disconnectUser(player, message);
			}
		}).join();
	}

	/*@Override
	public CentralisedFuture<Set<OnlineTarget>> getOnlineTargets() {
		return env.core.getFuturesFactory().supplySync(() -> {
			Set<OnlineTarget> result = new HashSet<>();
			for (Player player : env.getPlugin().getServer().getOnlinePlayers()) {
				result.add(new SpigotOnlineTarget(env, player));
			}
			return result;
		});
	}*/
	
	private void enforce(Player player, TargetMatcher matcher) {
		if (matcher.kick()) {
			env.handle.disconnectUser(player, matcher.message());
		} else {
			env.handle.sendMessage(player, matcher.message());
		}
	}

	@Override
	public void enforceMatcher(TargetMatcher matcher) {
		env.core.getFuturesFactory().runSync(() -> {
			for (Player player : env.getPlugin().getServer().getOnlinePlayers()) {
				if (matcher.uuids().contains(player.getUniqueId())
						|| matcher.addresses().contains(player.getAddress().getAddress())) {
					enforce(player, matcher);
				}
			}
		}).join();
	}

}
