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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.PlatformHandle;
import space.arim.api.env.annote.PlatformPlayer;

import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AbstractEnvEnforcer;
import space.arim.libertybans.core.env.TargetMatcher;

import org.bukkit.Server;
import org.bukkit.entity.Player;

@Singleton
public class SpigotEnforcer extends AbstractEnvEnforcer {
	
	private final FactoryOfTheFuture futuresFactory;
	private final PlatformHandle handle;
	private final Server server;

	@Inject
	public SpigotEnforcer(InternalFormatter formatter, PlatformHandle handle, FactoryOfTheFuture futuresFactory,
			Server server) {
		super(formatter, handle);
		this.futuresFactory = futuresFactory;
		this.handle = handle;
		this.server = server;
	}
	
	private void runSyncNow(Runnable command) {
		futuresFactory.runSync(command).join();
	}
	
	@Override
	protected void sendToThoseWithPermission0(String permission, SendableMessage message) {
		runSyncNow(() -> {
			for (Player player : server.getOnlinePlayers()) {
				if (player.hasPermission(permission)) {
					handle.sendMessage(player, message);
				}
			}
		});
	}
	
	@Override
	public void doForPlayerIfOnline(UUID uuid, Consumer<@PlatformPlayer Object> callback) {
		runSyncNow(() -> {
			Player player = server.getPlayer(uuid);
			if (player != null) {
				callback.accept(player);
			}
		});
	}

	@Override
	public void enforceMatcher(TargetMatcher matcher) {
		runSyncNow(() -> {
			for (Player player : server.getOnlinePlayers()) {
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
