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
package space.arim.libertybans.core.env;

import java.net.InetAddress;
import java.util.UUID;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import space.arim.api.env.annote.PlatformPlayer;

/**
 * Platform specific enforcer
 * 
 * @param <P> the player object type
 */
public interface EnvEnforcer<@PlatformPlayer P> {
	
	/**
	 * For all players with the specified permission, sends the message. <br>
	 * Automatically prefixes the given message.
	 * 
	 * @param permission the permission
	 * @param message the message
	 */
	void sendToThoseWithPermission(String permission, ComponentLike message);
	
	/**
	 * Searches for a player with the given uuid, if found, invokes the callback
	 * 
	 * @param uuid the uuid
	 * @param callback the callback
	 */
	void doForPlayerIfOnline(UUID uuid, Consumer<P> callback);

	/**
	 * Kicks the given player
	 *
	 * @param player the player to kick
	 * @param message the kick message
	 */
	void kickPlayer(P player, Component message);

	/**
	 * Sends a message to the given player. Does not include a prefix
	 *
	 * @param player the player
	 * @param message the message to send
	 */
	void sendMessageNoPrefix(P player, ComponentLike message);
	
	/**
	 * Enforces a target matcher, invoking its callback for players matching its uuid or address set
	 * 
	 * @param matcher the target matcher
	 */
	void enforceMatcher(TargetMatcher<P> matcher);
	
	/**
	 * Gets the uuid of a player
	 * 
	 * @param player the player
	 * @return the uuid
	 */
	UUID getUniqueIdFor(P player);
	
	/**
	 * Gets the address of a player
	 * 
	 * @param player the player
	 * @return the address
	 */
	InetAddress getAddressFor(P player);
	
}
