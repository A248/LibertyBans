/*
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum CommandType {
	
	BAN(PunishmentType.BAN, "ban.type.uuid"),
	TEMPBAN(PunishmentType.BAN, "ban.type.uuid"),
	UNBAN(PunishmentType.BAN, CommandCategory.REMOVE, "ban.undo.type.uuid"),
	IPBAN(PunishmentType.BAN, true, "ban.type.ip"),
	IPTEMPBAN(PunishmentType.BAN, true, "ban.type.ip"),
	IPUNBAN(PunishmentType.BAN, CommandCategory.REMOVE, true, "ban.undo.type.ip"),
	
	MUTE(PunishmentType.MUTE, "mute.type.uuid"),
	TEMPMUTE(PunishmentType.MUTE, "mute.type.uuid"),
	UNMUTE(PunishmentType.MUTE, CommandCategory.REMOVE, "mute.undo.type.uuid"),
	IPMUTE(PunishmentType.MUTE, true, "mute.type.ip"),
	IPTEMPMUTE(PunishmentType.MUTE, true, "mute.type.ip"),
	IPUNMUTE(PunishmentType.MUTE, CommandCategory.REMOVE, true, "mute.undo.type.ip"),
	
	WARN(PunishmentType.WARN, "warn.type.uuid"),
	TEMPWARN(PunishmentType.WARN, "warn.type.uuid"),
	UNWARN(PunishmentType.WARN, CommandCategory.REMOVE, "warn.undo.type.uuid"),
	IPWARN(PunishmentType.WARN, true, "warn.type.ip"),
	IPTEMPWARN(PunishmentType.WARN, true, "warn.type.ip"),
	IPUNWARN(PunishmentType.WARN, CommandCategory.REMOVE, true, "warn.undo.type.ip"),
	
	KICK(PunishmentType.KICK, "kick.type.uuid"),
	IPKICK(PunishmentType.KICK, true, "kick.type.ip"),
	
	ALLBANLIST(PunishmentType.BAN, CommandCategory.LIST, "banlist.type.all"),
	BANLIST(PunishmentType.BAN, CommandCategory.LIST, "banlist.type.uuid"),
	IPBANLIST(PunishmentType.BAN, CommandCategory.LIST, true, "banlist.type.ip"),
	ALLMUTELIST(PunishmentType.MUTE, CommandCategory.LIST, "mutelist.type.all"),
	MUTELIST(PunishmentType.MUTE, CommandCategory.LIST, "mutelist.type.uuid"),
	IPMUTELIST(PunishmentType.MUTE, CommandCategory.LIST, true, "mutelist.type.ip"),
	
	HISTORY("history.type.uuid"),
	IPHISTORY(true, "history.type.ip"),
	WARNS("warns.type.uuid"),
	IPWARNS(true, "warns.type.ip"),
	
	CHECK("check.type.uuid"),
	IPCHECK(true, "check.type.ip");
	
	private enum CommandCategory {
		ADD,
		REMOVE,
		LIST;
	}
	
	private final List<PunishmentType> types;
	private final CommandCategory category;
	private final boolean preferIp;
	private final String permission;
	
	private CommandType(final PunishmentType[] types, final CommandCategory category, final boolean preferIp, final String permission) {
		this.types = Arrays.asList(types);
		this.category = category;
		this.preferIp = preferIp;
		this.permission = permission;
	}
	
	private CommandType(final PunishmentType type, final CommandCategory category, final boolean preferIp, final String permission) {
		this(new PunishmentType[] {type}, category, preferIp, permission);
	}
	
	private CommandType(final PunishmentType type, final boolean preferIp, final String permission) {
		this(type, CommandCategory.ADD, preferIp, permission);
	}
	
	private CommandType(final PunishmentType type, final CommandCategory category, final String permission) {
		this(type, category, false, permission);
	}
	
	private CommandType(final PunishmentType type, final String permission) {
		this(type, CommandCategory.ADD, false, permission);
	}
	
	private CommandType(final boolean preferIp, final String permission) {
		this(PunishmentType.values(), CommandCategory.LIST, preferIp, permission);
	}
	
	private CommandType(final String permission) {
		this(false, permission);
	}
	
	public String permission() {
		return permission;
	}
	
	public boolean preferIp() {
		return preferIp;
	}
	
	public List<PunishmentType> applicableTypes() {
		return types;
	}
	
	public boolean isAddition() {
		return category.equals(CommandCategory.ADD);
	}
	
	public boolean isRemoval() {
		return category.equals(CommandCategory.REMOVE);
	}
	
	public boolean isListing() {
		return category.equals(CommandCategory.LIST);
	}
	
	public boolean isModif() {
		return isAddition() || isRemoval();
	}
	
	public boolean isGeneralListing() {
		return isListing() && name().contains("LIST");
	}
	
	public static Set<CommandType> allFor(PunishmentType type) {
		Set<CommandType> applicable = new HashSet<CommandType>();
		for (CommandType cmd : CommandType.values()) {
			if (cmd.applicableTypes().contains(type)) {
				applicable.add(cmd);
			}
		}
		return applicable;
	}

}
