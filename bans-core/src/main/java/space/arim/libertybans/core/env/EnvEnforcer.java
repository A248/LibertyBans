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

package space.arim.libertybans.core.env;

import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import space.arim.api.env.annote.PlatformPlayer;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

/**
 * Platform specific enforcer. Use of each method requires careful attention to
 * the return type. If the return type is a future, the method is safe to call
 * from any thread. Otherwise, the method is <i>ONLY</i> safe to call inside
 * an enforcement callback.
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
	CentralisedFuture<Void> sendToThoseWithPermission(String permission, ComponentLike message);

	/**
	 * Searches for a player with the given uuid, if found, invokes the callback
	 * 
	 * @param uuid the uuid
	 * @param callback the callback
	 */
	CentralisedFuture<Void> doForPlayerIfOnline(UUID uuid, Consumer<P> callback);

	/**
	 * Kicks the given player. <br>
	 * <br>
	 * <b>Must be used within a callback.</b>
	 *
	 * @param player the player to kick
	 * @param message the kick message
	 */
	void kickPlayer(P player, Component message);

	/**
	 * Sends a message to the given player. Does not include a prefix. <br>
	 * <br>
	 * <b>Must be used within a callback.</b>
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
	CentralisedFuture<Void> enforceMatcher(TargetMatcher<P> matcher);
	
	/**
	 * Gets the uuid of a player. <br>
	 * <br>
	 * <b>Must be used within a callback.</b>
	 * 
	 * @param player the player
	 * @return the uuid
	 */
	UUID getUniqueIdFor(P player);

	/**
	 * Gets the address of a player. <br>
	 * <br>
	 * <b>Must be used within a callback.</b>
	 * 
	 * @param player the player
	 * @return the address
	 */
	InetAddress getAddressFor(P player);

	/**
	 * Executes a command on the behalf of the console
	 *
	 * @param command the command to execute, without the slash
	 */
	// The looser return type of CompletableFuture allows for some flexibility in implementation
	CompletableFuture<Void> executeConsoleCommand(String command);

}
