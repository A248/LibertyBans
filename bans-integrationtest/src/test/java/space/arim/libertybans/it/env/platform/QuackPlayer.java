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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.api.chat.SendableMessage;
import space.arim.omnibus.util.ThisClass;

import java.net.InetAddress;
import java.util.Set;
import java.util.UUID;

public class QuackPlayer {

	private final QuackPlatform platform;
	private final UUID uuid;
	private final String name;
	private final InetAddress address;
	
	private final Set<String> permissions;
	
	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	QuackPlayer(QuackPlatform platform,
					   UUID uuid, String name, InetAddress address,
					   Set<String> permissions) {
		this.platform = platform;
		this.uuid = uuid;
		this.name = name;
		this.address = address;
		this.permissions = permissions;
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

	public boolean isStillOnline() {
		return platform.getPlayer(uuid) != null;
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
