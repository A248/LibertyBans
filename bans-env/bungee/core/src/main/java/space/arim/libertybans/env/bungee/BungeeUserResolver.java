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

package space.arim.libertybans.env.bungee;

import jakarta.inject.Inject;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import space.arim.libertybans.core.env.SimpleEnvUserResolver;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class BungeeUserResolver extends SimpleEnvUserResolver {

	private final FactoryOfTheFuture futuresFactory;
	private final ProxyServer server;
	private final AddressReporter addressReporter;

	@Inject
	public BungeeUserResolver(FactoryOfTheFuture futuresFactory, ProxyServer server, AddressReporter addressReporter) {
		this.futuresFactory = futuresFactory;
		this.server = server;
		this.addressReporter = addressReporter;
	}

	@Override
	protected <U> CentralisedFuture<U> performLookup(Supplier<U> rootImplementation) {
		return futuresFactory.completedFuture(rootImplementation.get());
	}

	@Override
	public Optional<UUID> lookupUUID0(String name) {
		return Optional.ofNullable(server.getPlayer(name)).map(ProxiedPlayer::getUniqueId);
	}

	@Override
	public Optional<String> lookupName0(UUID uuid) {
		return Optional.ofNullable(server.getPlayer(uuid)).map(ProxiedPlayer::getName);
	}

	@Override
	public Optional<InetAddress> lookupAddress0(String name) {
		return Optional.ofNullable(server.getPlayer(name)).map(addressReporter::getAddress);
	}

	@Override
	public Optional<UUIDAndAddress> lookupPlayer0(String name) {
		return Optional.ofNullable(server.getPlayer(name))
				.map((player) -> new UUIDAndAddress(player.getUniqueId(), addressReporter.getAddress(player)));
	}

	@Override
	public Optional<InetAddress> lookupCurrentAddress0(UUID uuid) {
		return Optional.ofNullable(server.getPlayer(uuid)).map(addressReporter::getAddress);
	}

}
