/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

import java.util.stream.Stream;

import net.kyori.adventure.text.ComponentLike;
import space.arim.libertybans.api.Operator;

public interface CmdSender {

	Operator getOperator();

	boolean hasPermission(String permission);

	void sendMessageNoPrefix(ComponentLike message);

	void sendMessage(ComponentLike message);

	void sendLiteralMessageNoPrefix(String messageToParse);

	void sendLiteralMessage(String messageToParse);

	/**
	 * Gets the names of other players in memory, for tab completion purposes. <br>
	 * <br>
	 * Must be called only on the same thread as where tab completion was requested by the platform.
	 *
	 * @return the names of other players
	 */
	Stream<String> getPlayerNames();

	/**
	 * Gets the names of other players on the same server as this player, for tab completion purposes. <br>
	 * <br>
	 * Must be called only on the same thread as where tab completion was requested by the platform.
	 * 
	 * @return the names of other players on the same server as this command sender
	 */
	Stream<String> getPlayerNamesOnSameServer();

}
