/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.it.util.RandomUtil;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class QuackPlayerBuilder {

	private final QuackPlatform platform;
	private Set<String> permissions = Set.of();

	QuackPlayerBuilder(QuackPlatform platform) {
		this.platform = Objects.requireNonNull(platform);
	}

	public QuackPlayerBuilder permissions(String...permissions) {
		this.permissions = Set.of(permissions);
		return this;
	}

	public QuackPlayer build(UUID uuid, String name, NetworkAddress address) {
		return new QuackPlayer(platform.getPlayerStore(), uuid, name, address.toInetAddress(), permissions);
	}

	public QuackPlayer buildRandomName(UUID uuid, NetworkAddress address) {
		return build(uuid, randomName(), address);
	}

	public QuackPlayer buildFullyRandom() {
		return buildRandomName(UUID.randomUUID(), RandomUtil.randomAddress());
	}

    private static final char[] VALID_NAME_CHARS = "qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM0123456789".toCharArray();
	private static String randomName() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int length = random.nextInt(4, 16);
        char[] chosen = new char[length];
        for (int n = 0; n < length; n++) {
            chosen[n] = VALID_NAME_CHARS[random.nextInt(VALID_NAME_CHARS.length)];
        }
        return String.valueOf(chosen);
	}
}
