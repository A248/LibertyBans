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
package space.arim.libertybans.core.config;

import java.util.Locale;

import net.kyori.adventure.text.Component;
import space.arim.api.jsonchat.adventure.util.ComponentText;

import space.arim.dazzleconf.annote.ConfDefault.DefaultInteger;
import space.arim.dazzleconf.annote.ConfDefault.DefaultString;
import space.arim.dazzleconf.annote.ConfDefault.DefaultStrings;
import space.arim.dazzleconf.annote.ConfHeader;
import space.arim.dazzleconf.annote.ConfKey;
import space.arim.dazzleconf.annote.IntegerRange;
import space.arim.dazzleconf.annote.SubSection;
import space.arim.libertybans.core.env.CmdSender;

@ConfHeader("Used for /banlist, /mutelist, /history, /warns, /blame")
public interface ListSection {

	interface PunishmentList {
		
		Component usage();
		
		int perPage();
		
		ComponentText noPages();
		
		ComponentText maxPages();
		
		Component permissionCommand();
		
		ComponentText layoutHeader();
		
		ComponentText layoutBody();
		
		ComponentText layoutFooter();
		
	}
	
	interface BanList extends PunishmentList {
		
		@Override
		@DefaultString("&cUsage: /banlist &e[page]")
		Component usage();
		
		@Override
		@IntegerRange(min = 1)
		@DefaultInteger(10)
		int perPage();
		
		@Override
		@DefaultString("&7There are no active bans.")
		ComponentText noPages();
		
		@Override
		@DefaultString("&7Page &e%PAGE%&7 does not exist.")
		ComponentText maxPages();
		
		@Override
		@ConfKey("permission.command")
		@DefaultString("&7You may not view the banlist.")
		Component permissionCommand();
		
		@Override
		@ConfKey("layout.header")
		@DefaultStrings({"&7[&eID&7] &e&oSubject",
				"&7Operator &8/ &7Reason &8/ &7Time Remaining",
				"&7"})
		ComponentText layoutHeader();
		
		@Override
		@ConfKey("layout.body")
		@DefaultStrings({"&7[&e%ID%&7] &e&o%VICTIM%",
				"&7%OPERATOR% &8/ &7%REASON% &8/ &7%TIME_REMAINING%",
				"&7"})
		ComponentText layoutBody();
		
		@Override
		@ConfKey("layout.footer")
		@DefaultStrings({"&7Page &e%PAGE%&7.||ttp:Click for next page||cmd:/libertybans banlist %NEXTPAGE%"})
		ComponentText layoutFooter();
		
	}
	
	interface MuteList extends PunishmentList {
		
		@Override
		@DefaultString("&cUsage: /mutelist &e[page]")
		Component usage();
		
		@Override
		@IntegerRange(min = 1)
		@DefaultInteger(10)
		int perPage();
		
		@Override
		@DefaultString("&7There are no active mutes.")
		ComponentText noPages();
		
		@Override
		@DefaultString("&7Page &e%PAGE%&7 does not exist.")
		ComponentText maxPages();
		
		@Override
		@ConfKey("permission.command")
		@DefaultString("&7You may not view the mutelist.")
		Component permissionCommand();
		
		@Override
		@ConfKey("layout.header")
		@DefaultStrings({"&7[&eID&7] &e&oSubject",
				"&7Operator &8/ &7Reason &8/ &7Time Remaining",
				"&7"})
		ComponentText layoutHeader();
		
		@Override
		@ConfKey("layout.body")
		@DefaultStrings({"&7[&e%ID%&7] &e&o%VICTIM%",
				"&7%OPERATOR% &8/ &7%REASON% &8/ &7%TIME_REMAINING%",
				"&7"})
		ComponentText layoutBody();
		
		@Override
		@ConfKey("layout.footer")
		@DefaultStrings({"&7Page &e%PAGE%&7.||ttp:Click for next page||cmd:/libertybans mutelist %NEXTPAGE%"})
		ComponentText layoutFooter();
		
	}
	
	interface History extends PunishmentList {
		
		@Override
		@DefaultString("&cUsage: /history &e<player> [page]")
		Component usage();
		
		@Override
		@IntegerRange(min = 1)
		@DefaultInteger(10)
		int perPage();
		
		@Override
		@DefaultString("&c&o%TARGET%&r&7 has no history.")
		ComponentText noPages();
		
		@Override
		@DefaultString("&7Page &e%PAGE%&7 does not exist.")
		ComponentText maxPages();
		
		@Override
		@ConfKey("permission.command")
		@DefaultString("&7You may not view history.")
		Component permissionCommand();
		
		@Override
		@ConfKey("layout.header")
		@DefaultStrings({"&7[&eID&7] &r&8/ &7Punishment Type",
				"&7Operator &8/ &7Reason &8/ &7Date Enacted",
				"&7"})
		ComponentText layoutHeader();
		
		@Override
		@ConfKey("layout.body")
		@DefaultStrings({"&7[&e%ID%&7] &r&7/ &7%TYPE%",
				"&7%OPERATOR% &8/ &7%REASON% &8/ &7%START_DATE%",
				"&7"})
		ComponentText layoutBody();
		
		@Override
		@ConfKey("layout.footer")
		@DefaultStrings({"&7Page &e%PAGE%&7.||ttp:Click for next page||cmd:/libertybans history %TARGET% %NEXTPAGE%"})
		ComponentText layoutFooter();
		
	}
	
	interface Warns extends PunishmentList {
		
		@Override
		@DefaultString("&cUsage: /warns &e<player> [page]")
		Component usage();
		
		@Override
		@IntegerRange(min = 1)
		@DefaultInteger(10)
		int perPage();
		
		@Override
		@DefaultString("&c&o%TARGET%&r&7 has no warns.")
		ComponentText noPages();
		
		@Override
		@DefaultString("&7Page &e%PAGE%&7 does not exist.")
		ComponentText maxPages();
		
		@Override
		@ConfKey("permission.command")
		@DefaultString("&7You may not view warns.")
		Component permissionCommand();
		
		@Override
		@ConfKey("layout.header")
		@DefaultStrings({"&7[&eID&7] Operator &8/ &7Reason &8/ &7Time Remaining",
				"&7"})
		ComponentText layoutHeader();
		
		@Override
		@ConfKey("layout.body")
		@DefaultStrings({"&7[&e%ID%&7] %OPERATOR% &8/ &7%REASON% &8/ &7%TIME_REMAINING%",
				"&7"})
		ComponentText layoutBody();
		
		@Override
		@ConfKey("layout.footer")
		@DefaultStrings({"&7Page &e%PAGE%&7.||ttp:Click for next page||cmd:/libertybans warns %TARGET% %NEXTPAGE%"})
		ComponentText layoutFooter();
		
	}
	
	interface Blame extends PunishmentList {
		
		@Override
		@DefaultString("&cUsage: /blame &e<player> [page]")
		Component usage();
		
		@Override
		@IntegerRange(min = 1)
		@DefaultInteger(10)
		int perPage();
		
		@Override
		@DefaultString("&c&o%TARGET%&r&7 has not punished any players.")
		ComponentText noPages();
		
		@Override
		@DefaultString("&7Page &e%PAGE%&7 does not exist.")
		ComponentText maxPages();
		
		@Override
		@ConfKey("permission.command")
		@DefaultString("&7You may not use blame.")
		Component permissionCommand();
		
		@Override
		@ConfKey("layout.header")
		@DefaultStrings({"&7[&eID&7] &e&oSubject &r&8/ &7Punishment Type",
				"&7Reason &8/ &7Date Enacted",
				"&7"})
		ComponentText layoutHeader();
		
		@Override
		@ConfKey("layout.body")
		@DefaultStrings({"&7[&e%ID%&7] &e&o%VICTIM% &r&8 / &7%TYPE%",
				"&7%REASON% &8/ &7%START_DATE%",
				"&7"})
		ComponentText layoutBody();
		
		@Override
		@ConfKey("layout.footer")
		@DefaultStrings({"&7Page &e%PAGE%&7.||ttp:Click for next page||cmd:/libertybans warns %TARGET% %NEXTPAGE%"})
		ComponentText layoutFooter();
		
	}
	
	enum ListType {
		
		BANLIST,
		MUTELIST,
		HISTORY,
		WARNS,
		BLAME;
		
		public boolean requiresTarget() {
			switch (this) {
			case BANLIST:
			case MUTELIST:
				return false;
			case HISTORY:
			case WARNS:
			case BLAME:
				return true;
			default:
				throw new IllegalArgumentException("requiresTarget not up-to-date");
			}
		}

		public boolean hasPermission(CmdSender sender) {
			return sender.hasPermission("libertybans.list." + this);
		}

		public static ListType fromString(String listType) {
			return valueOf(listType.toUpperCase(Locale.ROOT));
		}

		@Override
		public String toString() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
	
	default PunishmentList forType(ListType type) {
		switch (type) {
		case BANLIST:
			return banList();
		case MUTELIST:
			return muteList();
		case HISTORY:
			return history();
		case WARNS:
			return warns();
		case BLAME:
			return blame();
		default:
			throw new IllegalArgumentException("Unknown list type " + type);
		}
	}

	@ConfKey("ban-list")
	@SubSection
	BanList banList();

	@ConfKey("mute-list")
	@SubSection
	MuteList muteList();
	
	@SubSection
	History history();
	
	@SubSection
	Warns warns();
	
	@SubSection
	Blame blame();
	
}
