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
package space.arim.libertybans.core.commands;

import space.arim.api.chat.SendableMessage;
import space.arim.api.chat.serialiser.LegacyCodeSerialiser;
import space.arim.api.chat.serialiser.SendableMessageSerialiser;

enum UsageSection {

	PUNISH("&e/ban &7- ban players",
			"&e/mute &7- mute players",
			"&e/warn &7- warn players",
			"&e/kick &7- kick players"),
	UNPUNISH("&e/unban &7- undo a ban",
			"&e/unmute &7- undo a mute",
			"&e/unwarn &7- undo a warn"),
	LIST("&b/banlist &7- view active bans",
			"&b/mutelist &7- view active mutes",
			"&b/history &7- view a player's history",
			"&b/warns &7- view a player's warns",
			"&b/blame &7- view another staff member's punishments"),
	OTHER("&7/libertybans usage - shows this",
			"&7/libertybans reload - reload config.yml and messages configuration",
			"&7/libertybans restart - perform a full restart, reloads everything including database connections");
	
	private final SendableMessage content;
	
	UsageSection(String...commands) {
		String name = name();
		String headerString = "&b" + Character.toUpperCase(name.charAt(0)) + name.substring(1) + " commands:";

		SendableMessageSerialiser serialiser = LegacyCodeSerialiser.getInstance('&');
		SendableMessage header = serialiser.deserialise(headerString);
		SendableMessage body = serialiser.deserialise("\n" + String.join("\n", commands));
		content = header.concatenate(body);
	}
	
	SendableMessage getContent() {
		return content;
	}
	
}
