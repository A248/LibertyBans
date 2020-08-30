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

import java.util.UUID;
import java.util.function.Consumer;

import space.arim.api.chat.SendableMessage;
import space.arim.api.env.annote.PlatformPlayer;

/**
 * Platform specific enforcer
 * 
 * @author A248
 *
 */
public interface EnvEnforcer {
	
	/**
	 * For all players with the specified permission, sends the message
	 * 
	 * @param permission the permission
	 * @param message the message
	 */
	void sendToThoseWithPermission(String permission, SendableMessage message);
	
	/**
	 * Searches for a player with the given UUID, if found, invokes the callback
	 * 
	 * @param uuid the uuid
	 * @param callback the callback
	 */
	void doForPlayerIfOnline(UUID uuid, Consumer<@PlatformPlayer Object> callback);

	/**
	 * Searches for a player with the given UUID, if found, kicks the player with the given message
	 * 
	 * @param uuid the uuid
	 * @param message the kick message
	 */
	void kickByUUID(UUID uuid, SendableMessage message);
	
	/**
	 * Searches for a player with the given UUID, if found, sends the player the given message
	 * 
	 * @param uuid the uuid
	 * @param message the message
	 */
	void sendMessageByUUID(UUID uuid, SendableMessage message);
	
	void enforceMatcher(TargetMatcher matcher);
	
	UUID getUniqueIdFor(@PlatformPlayer Object player);
	
	byte[] getAddressFor(@PlatformPlayer Object player);
	
}
