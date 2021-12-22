/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.punish;

import java.net.InetAddress;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.punish.Punishment;

public interface Enforcer {

	/**
	 * Enforces a punishment
	 * 
	 * @param punishment the punishment
	 * @return a future completed when enforcement is finished
	 */
	CentralisedFuture<?> enforce(Punishment punishment);
	
	/**
	 * Enforces an incoming connection, returning a punishment message if denied, null if allowed. <br>
	 * <br>
	 * Adds the uuidField and name to the local fast cache, queries for an applicable ban, and formats the
	 * ban reason as the punishment message.
	 * 
	 * @param uuid the player's uuidField
	 * @param name the player's name
	 * @param address the player's network address
	 * @return a future which yields the punishment message if denied, else null if allowed
	 */
	CentralisedFuture<Component> executeAndCheckConnection(UUID uuid, String name, NetworkAddress address);
	
	/**
	 * Enforces an incoming connection, returning a punishment message if denied, null if allowed. <br>
	 * <br>
	 * Adds the uuidField and name to the local fast cache, queries for an applicable ban, and formats the
	 * ban reason as the punishment message.
	 * 
	 * @param uuid the player's uuidField
	 * @param name the player's name
	 * @param address the player's network address
	 * @return a future which yields the punishment message if denied, else null if allowed
	 */
	default CentralisedFuture<Component> executeAndCheckConnection(UUID uuid, String name, InetAddress address) {
		return executeAndCheckConnection(uuid, name, NetworkAddress.of(address));
	}
	
	/**
	 * Enforces a chat message or executed command, returning a punishment message if denied, null if allowed. <br>
	 * <br>
	 * If this corresponds to an executed command, the configured commands whose access to muted players to block
	 * are taken into account.
	 * 
	 * @param uuid the player's uuidField
	 * @param address the player's network address
	 * @param command the command executed, or {@code null} if this is a chat message
	 * @return a future which yields the punishment message if denied, else null if allowed
	 */
	CentralisedFuture<Component> checkChat(UUID uuid, NetworkAddress address, String command);
	
	/**
	 * Enforces a chat message or executed command, returning a punishment message if denied, null if allowed. <br>
	 * <br>
	 * If this corresponds to an executed command, the configured commands whose access to muted players to block
	 * are taken into account.
	 * 
	 * @param uuid the player's uuidField
	 * @param address the player's network address
	 * @param command the command executed, or {@code null} if this is a chat message
	 * @return a future which yields the punishment message if denied, else null if allowed
	 */
	default CentralisedFuture<Component> checkChat(UUID uuid, InetAddress address, String command) {
		return checkChat(uuid, NetworkAddress.of(address), command);
	}
	
}
