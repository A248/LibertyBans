/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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
package space.arim.libertybans.it.env;

import jakarta.inject.Inject;
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.env.UUIDAndAddress;
import space.arim.libertybans.it.env.platform.QuackPlatform;
import space.arim.libertybans.it.env.platform.QuackPlayer;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;

public class QuackUserResolver implements EnvUserResolver {

	private final QuackPlatform platform;

	@Inject
	public QuackUserResolver(QuackPlatform platform) {
		this.platform = platform;
	}

	@Override
	public Optional<UUID> lookupUUID(String name) {
		return platform.getPlayer(name).map(QuackPlayer::getUniqueId);
	}

	@Override
	public Optional<String> lookupName(UUID uuid) {
		return platform.getPlayer(uuid).map(QuackPlayer::getName);
	}

	@Override
	public Optional<InetAddress> lookupAddress(String name) {
		return platform.getPlayer(name).map(QuackPlayer::getAddress);
	}

	@Override
	public Optional<UUIDAndAddress> lookupPlayer(String name) {
		return platform.getPlayer(name)
				.map((player) -> new UUIDAndAddress(player.getUniqueId(), player.getAddress()));
	}

}
