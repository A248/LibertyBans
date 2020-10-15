/* 
 * LibertyBans-integrationtest
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-integrationtest is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-integrationtest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-integrationtest. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.it.env.platform;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;

import space.arim.api.chat.SendableMessage;

public class QuackPlayer {

	private final QuackPlatform platform;
	private final UUID uuid = UUID.randomUUID();
	private final String name;
	private final InetAddress address;
	
	private final Set<String> permissions;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
	
	public QuackPlayer(QuackPlatform platform, String...permissions) {
		this.platform = platform;

		Random random = ThreadLocalRandom.current();

		name = new String(randomBytes(random, 16), StandardCharsets.UTF_8);
		try {
			address = InetAddress.getByAddress(randomBytes(random, 4));
		} catch (UnknownHostException ex) {
			throw new IllegalStateException(ex);
		}
		this.permissions = Set.of(permissions);
	}
	
	private static byte[] randomBytes(Random random, int length) {
		byte[] bytes = new byte[length];
		random.nextBytes(bytes);
		return bytes;
	}
	
	public UUID getUniqueId() {
		return uuid;
	}
	
	public String getName() {
		return name;
	}
	
	public InetAddress getAddress() {
		return address;
	}
	
	public boolean hasPermission(String permission) {
		return permissions.contains(permission);
	}
	
	public void sendMessage(SendableMessage msg) {
		logger.info("{} received '{}'", name, platform.toDisplay(msg));
	}
	
	public void kickPlayer(SendableMessage msg) {
		platform.remove(this, msg);
	}
	
}
