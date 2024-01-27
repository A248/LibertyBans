/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.env.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import jakarta.inject.Inject;
import space.arim.libertybans.core.env.SimpleEnvUserResolver;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class VelocityUserResolver extends SimpleEnvUserResolver {

	private final FactoryOfTheFuture futuresFactory;
	private final ProxyServer server;

	@Inject
	public VelocityUserResolver(FactoryOfTheFuture futuresFactory, ProxyServer server) {
		this.futuresFactory = futuresFactory;
		this.server = server;
	}

	@Override
	protected <U> CentralisedFuture<U> performLookup(Supplier<U> rootImplementation) {
		return futuresFactory.completedFuture(rootImplementation.get());
	}

	@Override
	public Optional<UUID> lookupUUID0(String name) {
		return server.getPlayer(name).map(Player::getUniqueId);
	}

	@Override
	public Optional<String> lookupName0(UUID uuid) {
		return server.getPlayer(uuid).map(Player::getUsername);
	}

	@Override
	public Optional<InetAddress> lookupAddress0(String name) {
		return server.getPlayer(name).map((player) -> player.getRemoteAddress().getAddress());
	}

	@Override
	public Optional<UUIDAndAddress> lookupPlayer0(String name) {
		return server.getPlayer(name)
				.map((player) -> new UUIDAndAddress(player.getUniqueId(), player.getRemoteAddress().getAddress()));
	}

	@Override
	public Optional<InetAddress> lookupCurrentAddress0(UUID uuid) {
		return server.getPlayer(uuid).map((player) -> player.getRemoteAddress().getAddress());
	}

}
