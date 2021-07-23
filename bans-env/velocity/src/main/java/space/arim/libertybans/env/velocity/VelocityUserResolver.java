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
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import space.arim.libertybans.core.env.EnvUserResolver;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import space.arim.libertybans.core.env.UUIDAndAddress;

public class VelocityUserResolver implements EnvUserResolver {

	private final ProxyServer server;

	@Inject
	public VelocityUserResolver(ProxyServer server) {
		this.server = server;
	}

	@Override
	public Optional<UUID> lookupUUID(String name) {
		return server.getPlayer(name).map(Player::getUniqueId);
	}

	@Override
	public Optional<String> lookupName(UUID uuid) {
		return server.getPlayer(uuid).map(Player::getUsername);
	}

	@Override
	public Optional<InetAddress> lookupAddress(String name) {
		return server.getPlayer(name).map((player) -> player.getRemoteAddress().getAddress());
	}

	@Override
	public Optional<UUIDAndAddress> lookupPlayer(String name) {
		return server.getPlayer(name)
				.map((player) -> new UUIDAndAddress(player.getUniqueId(), player.getRemoteAddress().getAddress()));
	}

}
