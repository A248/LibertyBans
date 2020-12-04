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

import java.util.Set;

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.api.Operator;

public interface CmdSender {

	Operator getOperator();
	
	boolean hasPermission(String permission);
	
	void sendMessageNoPrefix(SendableMessage message);
	
	void sendMessage(SendableMessage message);
	
	void sendLiteralMessageNoPrefix(String messageToParse);
	
	void sendLiteralMessage(String messageToParse);
	
	Object getRawSender();
	
	/**
	 * Gets the names of other players on the same server as this player. <br>
	 * Used for tab completions
	 * 
	 * @return the names of other players on the same server as this command sender
	 */
	Set<String> getOtherPlayersOnSameServer();
	
}
