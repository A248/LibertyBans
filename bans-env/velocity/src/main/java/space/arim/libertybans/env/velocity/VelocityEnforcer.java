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

import java.net.InetAddress;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.PlatformHandle;
import space.arim.api.env.annote.PlatformPlayer;

import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.env.AbstractEnvEnforcer;
import space.arim.libertybans.core.env.TargetMatcher;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

@Singleton
public class VelocityEnforcer extends AbstractEnvEnforcer {

	private final PlatformHandle handle;
	private final ProxyServer server;
	
	@Inject
	public VelocityEnforcer(InternalFormatter formatter, PlatformHandle handle, ProxyServer server) {
		super(formatter, handle);
		this.handle = handle;
		this.server = server;
	}

	@Override
	protected void sendToThoseWithPermission0(String permission, SendableMessage message) {
		for (Player player : server.getAllPlayers()) {
			if (player.hasPermission(permission)) {
				handle.sendMessage(player, message);
			}
		}
	}

	@Override
	public void doForPlayerIfOnline(UUID uuid, Consumer<@PlatformPlayer Object> callback) {
		server.getPlayer(uuid).ifPresent(callback);
	}

	@Override
	public void enforceMatcher(TargetMatcher matcher) {
		for (Player player : server.getAllPlayers()) {
			if (matcher.matches(player.getUniqueId(), player.getRemoteAddress().getAddress())) {
				matcher.callback().accept(player);
			}
		}
	}

	@Override
	public UUID getUniqueIdFor(@PlatformPlayer Object player) {
		return ((Player) player).getUniqueId();
	}

	@Override
	public InetAddress getAddressFor(@PlatformPlayer Object player) {
		return ((Player) player).getRemoteAddress().getAddress();
	}

}
