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

package space.arim.libertybans.env.spigot;

import jakarta.inject.Inject;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import space.arim.libertybans.core.env.SimpleEnvUserResolver;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.morepaperlib.MorePaperLib;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class SpigotUserResolver extends SimpleEnvUserResolver {

	private final FactoryOfTheFuture futuresFactory;
	private final Server server;
	private final MorePaperLib morePaperLib;

	@Inject
	public SpigotUserResolver(FactoryOfTheFuture futuresFactory, Server server, MorePaperLib morePaperLib) {
		this.futuresFactory = futuresFactory;
		this.server = server;
		this.morePaperLib = morePaperLib;
	}

	@Override
	protected <U> CentralisedFuture<U> performLookup(Supplier<U> rootImplementation) {
		if (morePaperLib.scheduling().isUsingFolia()) {
			return futuresFactory.completedFuture(rootImplementation.get());
		}
		return futuresFactory.supplySync(rootImplementation);
	}

	@Override
	public Optional<UUID> lookupUUID0(String name) {
		return Optional.ofNullable(server.getPlayerExact(name)).map(Player::getUniqueId);
	}

	@Override
	public Optional<String> lookupName0(UUID uuid) {
		return Optional.ofNullable(server.getPlayer(uuid)).map(Player::getName);
	}

	@Override
	public Optional<InetAddress> lookupAddress0(String name) {
		return Optional.ofNullable(server.getPlayerExact(name)).map((player) -> player.getAddress().getAddress());
	}

	@Override
	public Optional<UUIDAndAddress> lookupPlayer0(String name) {
		return Optional.ofNullable(server.getPlayerExact(name))
				.map((player) -> new UUIDAndAddress(player.getUniqueId(), player.getAddress().getAddress()));
	}

	@Override
	public Optional<InetAddress> lookupCurrentAddress0(UUID uuid) {
		return Optional.ofNullable(server.getPlayer(uuid)).map((player) -> player.getAddress().getAddress());
	}

}
