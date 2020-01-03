/* 
 * ArimBansLib, an API for ArimBans
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansLib. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api;

import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.api.exception.TypeParseException;

public enum CommandType {
	
	BAN(SubCategory.BAN),
	UNBAN(SubCategory.UNBAN),
	IPBAN(SubCategory.BAN, IpSpec.IP),
	IPUNBAN(SubCategory.UNBAN, IpSpec.IP),
	
	MUTE(SubCategory.MUTE),
	UNMUTE(SubCategory.UNMUTE),
	IPMUTE(SubCategory.MUTE, IpSpec.IP),
	IPUNMUTE(SubCategory.UNMUTE, IpSpec.IP),
	
	WARN(SubCategory.WARN),
	UNWARN(SubCategory.UNWARN),
	IPWARN(SubCategory.WARN, IpSpec.IP),
	IPUNWARN(SubCategory.UNWARN, IpSpec.IP),
	
	KICK(SubCategory.KICK),
	IPKICK(SubCategory.KICK, IpSpec.IP),
	
	BANLIST(SubCategory.BANLIST),
	IPBANLIST(SubCategory.BANLIST, IpSpec.IP),
	PLAYERBANLIST(SubCategory.BANLIST, IpSpec.UUID),
	MUTELIST(SubCategory.MUTELIST),
	IPMUTELIST(SubCategory.MUTELIST, IpSpec.IP),
	PLAYERMUTELIST(SubCategory.MUTELIST, IpSpec.UUID),
	
	HISTORY(SubCategory.HISTORY),
	IPHISTORY(SubCategory.HISTORY, IpSpec.IP),
	WARNS(SubCategory.WARNS),
	IPWARNS(SubCategory.WARNS, IpSpec.IP),
	
	STATUS(SubCategory.STATUS),
	IPSTATUS(SubCategory.STATUS, IpSpec.IP),
	
	IPS(SubCategory.IPS, IpSpec.UUID),
	GEOIP(SubCategory.GEOIP, IpSpec.IP),
	ALTS(SubCategory.ALTS, IpSpec.IP),
	
	BLAME(SubCategory.BLAME, IpSpec.UUID),
	ROLLBACK(SubCategory.ROLLBACK, IpSpec.UUID),
	EDITREASON(SubCategory.EDITREASON, IpSpec.BOTH),
	RELOAD(SubCategory.RELOAD, IpSpec.BOTH);
	
	public enum SubCategory {
		BAN(Category.ADD, "ban.do"),
		UNBAN(Category.REMOVE, "ban.undo"),
		MUTE(Category.ADD, "mute.do"),
		UNMUTE(Category.REMOVE, "mute.undo"),
		WARN(Category.ADD, "warn.do"),
		UNWARN(Category.REMOVE, "warn.undo"),
		KICK(Category.ADD, "kick.do"),
		BANLIST(Category.LIST, "banlist", true),
		MUTELIST(Category.LIST, "mutelist", true),
		HISTORY(Category.LIST, "history"),
		WARNS(Category.LIST, "warns"),
		STATUS(Category.OTHER, "status"),
		IPS(Category.OTHER, "iplookup"),
		GEOIP(Category.OTHER, "geoip"),
		ALTS(Category.OTHER, "alts"),
		BLAME(Category.LIST, "blame"),
		ROLLBACK(Category.OTHER, "rollback"),
		EDITREASON(Category.OTHER, "editreason"),
		RELOAD(Category.OTHER, "reload", true, false);
		
		private final Category category;
		private final String permissionBase;
		private final boolean noArg;
		private final boolean reqAsync;
		
		private SubCategory(Category category, String permissionBase, boolean noArg, boolean reqAsync) {
			this.category = category;
			this.permissionBase = "arimbans." + permissionBase;
			this.noArg = noArg;
			this.reqAsync = reqAsync;
		}
		
		private SubCategory(Category category, String permissionBase, boolean noArg) {
			this(category, permissionBase, noArg, true);
		}
		
		private SubCategory(Category category, String permissionBase) {
			this(category, permissionBase, false);
		}
		
		public Category category() {
			return category;
		}
		
		String permissionBase() {
			return permissionBase;
		}
		
		boolean noTarget() {
			return noArg;
		}
		
		@Deprecated
		boolean requiresAsynchronisation() {
			return reqAsync;
		}
		
	}
	
	public enum Category {
		ADD,
		REMOVE,
		LIST,
		OTHER
	}
	
	public enum IpSpec {
		BOTH,
		UUID,
		IP
	}
	
	private SubCategory subCategory;
	private IpSpec ipSpec;
	
	private CommandType(SubCategory subCategory, IpSpec ipSpec) {
		this.subCategory = subCategory;
		this.ipSpec = ipSpec;
	}
	
	private CommandType(SubCategory subCategory) {
		this(subCategory, IpSpec.BOTH);
	}
	
	public Category category() {
		return subCategory().category();
	}
	
	public SubCategory subCategory() {
		return subCategory;
	}
	
	public IpSpec ipSpec() {
		return ipSpec;
	}
	
	public boolean canHaveNoTarget() {
		return subCategory().noTarget();
	}
	
	@Deprecated
	public boolean requiresAsynchronisation() {
		return subCategory().requiresAsynchronisation();
	}
	
	public String getPermission() {
		String base = subCategory().permissionBase();
		switch (ipSpec()) {
		case BOTH:
			return base + ".use";
		case UUID:
			return base + ".player";
		case IP:
			return base + ".ip";
		default:
			assert false;
			throw new IllegalStateException("IpSpec is invalid!");
		}
	}
	
	public CommandType alternateIpSpec() {
		for (CommandType alt : CommandType.values()) {
			if (alt.subCategory().equals(subCategory()) && alt.ipSpec().equals(IpSpec.IP)) {
				return alt;
			}
		}
		throw new InternalStateException("Could not find command with IpSpec.IP for subcategory " + subCategory());
	}
	
	public static CommandType parseCommand(String input) {
		for (CommandType type : CommandType.values()) {
			if (type.toString().equalsIgnoreCase(input)) {
				return type;
			}
		}
		throw new TypeParseException(input, CommandType.class);
	}

}
