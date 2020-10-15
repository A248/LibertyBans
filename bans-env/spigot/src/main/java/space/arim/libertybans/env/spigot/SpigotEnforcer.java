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

import java.net.InetAddress;
import java.util.UUID;
import java.util.function.Consumer;

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.annote.PlatformPlayer;

import space.arim.libertybans.core.env.AbstractEnvEnforcer;
import space.arim.libertybans.core.env.TargetMatcher;

import org.bukkit.entity.Player;

class SpigotEnforcer extends AbstractEnvEnforcer {

	SpigotEnforcer(SpigotEnv env) {
		super(env.core, env);
	}
	
	@Override
	protected SpigotEnv env() {
		return (SpigotEnv) super.env();
	}
	
	private void runSyncNow(Runnable command) {
		env().core.getFuturesFactory().runSync(command).join();
	}
	
	@Override
	protected void sendToThoseWithPermission0(String permission, SendableMessage message) {
		runSyncNow(() -> {
			for (Player player : env().getPlugin().getServer().getOnlinePlayers()) {
				if (player.hasPermission(permission)) {
					env().getPlatformHandle().sendMessage(player, message);
				}
			}
		});
	}
	
	@Override
	public void doForPlayerIfOnline(UUID uuid, Consumer<@PlatformPlayer Object> callback) {
		runSyncNow(() -> {
			Player player = env().getPlugin().getServer().getPlayer(uuid);
			if (player != null) {
				callback.accept(player);
			}
		});
	}

	@Override
	public void enforceMatcher(TargetMatcher matcher) {
		runSyncNow(() -> {
			for (Player player : env().getPlugin().getServer().getOnlinePlayers()) {
				if (matcher.matches(player.getUniqueId(), player.getAddress().getAddress())) {
					matcher.callback().accept(player);
				}
			}
		});
	}

	@Override
	public UUID getUniqueIdFor(@PlatformPlayer Object player) {
		return ((Player) player).getUniqueId();
	}

	@Override
	public InetAddress getAddressFor(@PlatformPlayer Object player) {
		return ((Player) player).getAddress().getAddress();
	}

}
