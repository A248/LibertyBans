/* 
 * LibertyBans-env-velocity
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-velocity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-velocity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-velocity. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.velocity;

import java.util.UUID;

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.core.env.EnvEnforcer;
import space.arim.libertybans.core.env.TargetMatcher;

import com.velocitypowered.api.proxy.Player;

class VelocityEnforcer implements EnvEnforcer {

	private final VelocityEnv env;
	
	VelocityEnforcer(VelocityEnv env) {
		this.env = env;
	}

	@Override
	public void sendToThoseWithPermission(String permission, SendableMessage message) {
		for (Player player : env.getServer().getAllPlayers()) {
			if (player.hasPermission(permission)) {
				env.handle.sendMessage(player, message);
			}
		}
	}

	@Override
	public void kickByUUID(UUID uuid, SendableMessage message) {
		Player player = env.getServer().getPlayer(uuid).orElse(null);
		if (player != null) {
			env.handle.disconnectUser(player, message);
		}
	}

	/*@Override
	public CentralisedFuture<Set<OnlineTarget>> getOnlineTargets() {
		Set<OnlineTarget> result = new HashSet<>();
		for (Player player : env.getServer().getAllPlayers()) {
			result.add(new VelocityOnlineTarget(env, player));
		}
		return env.core.getFuturesFactory().completedFuture(result);
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
		for (Player player : env.getServer().getAllPlayers()) {
			if (matcher.uuids().contains(player.getUniqueId())
					|| matcher.addresses().contains(player.getRemoteAddress().getAddress())) {
				enforce(player, matcher);
			}
		}
	}
	
}
