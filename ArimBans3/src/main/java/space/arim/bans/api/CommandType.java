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

public enum CommandType {
	
	BAN(PunishmentType.BAN, "ban.type.uuid", "ban.use.time.perm"),
	TEMPBAN(PunishmentType.BAN, "ban.type.uuid", "ban.use.time.temp"),
	UNBAN(PunishmentType.BAN, CommandCategory.REMOVE, "ban.undo.type.uuid"),
	IPBAN(PunishmentType.BAN, true, "ban.type.ip", "ban.use.time.perm"),
	IPTEMPBAN(PunishmentType.BAN, true, "ban.type.ip", "ban.use.time.temp"),
	IPUNBAN(PunishmentType.BAN, CommandCategory.REMOVE, true, "ban.undo.type.ip"),
	
	MUTE(PunishmentType.MUTE, "mute.type.uuid", "mute.use.time.perm"),
	TEMPMUTE(PunishmentType.MUTE, "mute.type.uuid", "mute.use.time.temp"),
	UNMUTE(PunishmentType.MUTE, CommandCategory.REMOVE, "mute.undo.type.uuid"),
	IPMUTE(PunishmentType.MUTE, true, "mute.type.ip", "mute.use.time.perm"),
	IPTEMPMUTE(PunishmentType.MUTE, true, "mute.type.ip", "mute.use.time.temp"),
	IPUNMUTE(PunishmentType.MUTE, CommandCategory.REMOVE, true, "mute.undo.type.ip"),
	
	WARN(PunishmentType.WARN, "warn.type.uuid", "warn.use.time.perm"),
	TEMPWARN(PunishmentType.WARN, "warn.type.uuid", "warn.use.time.temp"),
	UNWARN(PunishmentType.WARN, CommandCategory.REMOVE, "warn.undo.type.uuid"),
	IPWARN(PunishmentType.WARN, true, "warn.type.ip", "warn.use.time.perm"),
	IPTEMPWARN(PunishmentType.WARN, true, "warn.type.ip", "warn.use.time.temp"),
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
		LIST
	}
	
	private final PunishmentType[] types;
	private final CommandCategory category;
	private final boolean preferIp;
	private final String[] permissions;
	
	private CommandType(final PunishmentType[] types, final CommandCategory category, final boolean preferIp, final String[] permissions) {
		this.types = types;
		this.category = category;
		this.preferIp = preferIp;
		this.permissions = permissions;
	}
	
	private CommandType(final PunishmentType type, final CommandCategory category, final boolean preferIp, final String...permissions) {
		this(new PunishmentType[] {type}, category, preferIp, permissions);
	}
	
	private CommandType(final PunishmentType type, final boolean preferIp, final String...permissions) {
		this(type, CommandCategory.ADD, preferIp, permissions);
	}
	
	private CommandType(final PunishmentType type, final CommandCategory category, final String...permissions) {
		this(type, category, false, permissions);
	}
	
	private CommandType(final PunishmentType type, final String...permissions) {
		this(type, CommandCategory.ADD, false, permissions);
	}
	
	private CommandType(final boolean preferIp, final String...permissions) {
		this(PunishmentType.values(), CommandCategory.LIST, preferIp, permissions);
	}
	
	private CommandType(final String...permissions) {
		this(false, permissions);
	}
	
	public String[] permission() {
		return permissions;
	}
	
	public boolean preferIp() {
		return preferIp;
	}
	
	public PunishmentType[] applicableTypes() {
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

}
