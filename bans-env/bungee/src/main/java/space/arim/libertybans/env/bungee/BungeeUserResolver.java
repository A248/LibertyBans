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
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import space.arim.libertybans.core.env.EnvUserResolver;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import space.arim.libertybans.core.env.UUIDAndAddress;

public class BungeeUserResolver implements EnvUserResolver {

	private final ProxyServer server;
	private final AddressReporter addressReporter;

	@Inject
	public BungeeUserResolver(ProxyServer server, AddressReporter addressReporter) {
		this.server = server;
		this.addressReporter = addressReporter;
	}

	@Override
	public Optional<UUID> lookupUUID(String name) {
		return Optional.ofNullable(server.getPlayer(name)).map(ProxiedPlayer::getUniqueId);
	}

	@Override
	public Optional<String> lookupName(UUID uuid) {
		return Optional.ofNullable(server.getPlayer(uuid)).map(ProxiedPlayer::getName);
	}

	@Override
	public Optional<InetAddress> lookupAddress(String name) {
		return Optional.ofNullable(server.getPlayer(name)).map(addressReporter::getAddress);
	}

	@Override
	public Optional<UUIDAndAddress> lookupPlayer(String name) {
		return Optional.ofNullable(server.getPlayer(name))
				.map((player) -> new UUIDAndAddress(player.getUniqueId(), addressReporter.getAddress(player)));
	}

}
