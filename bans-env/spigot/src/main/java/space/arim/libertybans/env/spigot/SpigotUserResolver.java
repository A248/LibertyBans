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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import space.arim.libertybans.core.env.EnvUserResolver;

import org.bukkit.Server;
import org.bukkit.entity.Player;

public class SpigotUserResolver implements EnvUserResolver {

	private final FactoryOfTheFuture futuresFactory;
	private final Server server;

	@Inject
	public SpigotUserResolver(FactoryOfTheFuture futuresFactory, Server server) {
		this.futuresFactory = futuresFactory;
		this.server = server;
	}

	private <T> T getSync(Supplier<T> supplier) {
		return futuresFactory.supplySync(supplier).join();
	}

	@Override
	public Optional<UUID> lookupUUID(String name) {
		return getSync(() -> Optional.ofNullable(server.getPlayerExact(name)).map(Player::getUniqueId));
	}

	@Override
	public Optional<String> lookupName(UUID uuid) {
		return getSync(() -> Optional.ofNullable(server.getPlayer(uuid)).map(Player::getName));
	}

	@Override
	public Optional<InetAddress> lookupAddress(String name) {
		return getSync(() ->
				Optional.ofNullable(server.getPlayerExact(name)).map((player) -> player.getAddress().getAddress()));
	}

	@Override
	public Optional<UUIDAndAddress> lookupPlayer(String name) {
		return getSync(() -> Optional.ofNullable(server.getPlayerExact(name))
				.map((player) -> new UUIDAndAddress(player.getUniqueId(), player.getAddress().getAddress())));
	}

}
