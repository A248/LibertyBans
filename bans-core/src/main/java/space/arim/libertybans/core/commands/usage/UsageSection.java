/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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
package space.arim.libertybans.core.commands.usage;

import java.util.Locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import space.arim.api.jsonchat.adventure.ChatMessageComponentSerializer;

enum UsageSection {

	PUNISH("&e/ban, /ipban &7- ban players",
			"&e/mute, /ipmute &7- mute players",
			"&e/warn, /ipwarn &7- warn players",
			"&e/kick, /ipkick &7- kick players"),
	UNPUNISH("&e/unban, /unbanip &7- undo a ban",
			"&e/unmute, /unmuteip &7- undo a mute",
			"&e/unwarn, /unwarnip &7- undo a warn"),
	LIST("&e/banlist &7- view active bans",
			"&e/mutelist &7- view active mutes",
			"&e/history &7- view a player's history",
			"&e/warns &7- view a player's warns",
			"&e/blame &7- view another staff member's punishments"),
	ALT_MANAGEMENT("&e/alts &7- detect alt accounts",
			"&e/accounthistory &7- list and modify stored join history"),
	OTHER("&e/libertybans &7usage - shows this",
			"&e/libertybans &7about - shows version and plugin information",
			"&e/libertybans &7debug - outputs debug information"),
	ADMIN("&e/libertybans &7reload - reload config.yml and language configuration",
			"&e/libertybans &7restart - perform a full restart; reloads everything including database connections",
			"&e/libertybans &7addon - manage installed addons",
			"&e/libertybans &7import - imports from another plugin");
	
	private final Component content;
	
	UsageSection(String...commands) {
		String name = name().replace("_", " ");
		String formattedName = name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
		String headerString = "&b" + formattedName + " commands:";

		ChatMessageComponentSerializer serializer = new ChatMessageComponentSerializer();
		Component[] components = new Component[1 + (2 * commands.length)];
		components[0] = serializer.deserialize(headerString);
		int n = 1;
		for (String command : commands) {
			components[n] = Component.newline();
			components[n + 1] = serializer.deserialize(command);
			n += 2;
		}
		content = TextComponent.ofChildren(components);
	}
	
	Component content() {
		return content;
	}
	
}
