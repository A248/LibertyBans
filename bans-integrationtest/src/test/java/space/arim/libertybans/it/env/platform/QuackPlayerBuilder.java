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

package space.arim.libertybans.it.env.platform;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class QuackPlayerBuilder {

	private final QuackPlatform platform;
	private Set<String> permissions = Set.of();

	public QuackPlayerBuilder(QuackPlatform platform) {
		this.platform = Objects.requireNonNull(platform);
	}

	public QuackPlayerBuilder permissions(String...permissions) {
		this.permissions = Set.of(permissions);
		return this;
	}

	public QuackPlayer build(UUID uuid, String name, InetAddress address) {
		var player = new QuackPlayer(platform, uuid, name, address, permissions);
		platform.addPlayer(player);
		return player;
	}

	public QuackPlayer buildRandomName(UUID uuid, InetAddress address) {
		return build(uuid, randomName(), address);
	}

	public QuackPlayer buildFullyRandom() {
		InetAddress address;
		try {
			address = InetAddress.getByAddress(randomBytes(4));
		} catch (UnknownHostException ex) {
			throw new IllegalStateException(ex);
		}
		return buildRandomName(UUID.randomUUID(), address);
	}

	private static String randomName() {
		return new String(randomBytes(16), StandardCharsets.UTF_8);
	}

	private static byte[] randomBytes(int length) {
		byte[] bytes = new byte[length];
		ThreadLocalRandom.current().nextBytes(bytes);
		return bytes;
	}

}
