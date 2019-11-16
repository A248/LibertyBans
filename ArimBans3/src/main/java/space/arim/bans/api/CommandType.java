package space.arim.bans.api;

public enum CommandType {
	
	BAN("ban.type.uuid", "ban.use.time.perm"),
	TEMPBAN("ban.type.uuid", "ban.use.time.temp"),
	UNBAN("ban.undo.type.uuid"),
	IPBAN(true, "ban.type.ip", "ban.use.time.perm"),
	IPTEMPBAN(true, "ban.type.ip", "ban.use.time.temp"),
	IPUNBAN(true, "ban.undo.type.ip"),
	
	MUTE("mute.type.uuid", "mute.use.time.perm"),
	TEMPMUTE("mute.type.uuid", "mute.use.time.temp"),
	UNMUTE("mute.undo.type.uuid"),
	IPMUTE(true, "mute.type.ip", "mute.use.time.perm"),
	IPTEMPMUTE(true, "mute.type.ip", "mute.use.time.temp"),
	IPUNMUTE(true, "mute.undo.type.ip"),
	
	WARN("warn.type.uuid", "warn.use.time.perm"),
	TEMPWARN("warn.type.uuid", "warn.use.time.temp"),
	UNWARN("warn.undo.type.uuid"),
	IPWARN(true, "warn.type.ip", "warn.use.time.perm"),
	IPTEMPWARN(true, "warn.type.ip", "warn.use.time.temp"),
	IPUNWARN(true, "warn.undo.type.ip"),
	
	KICK("kick.type.uuid"),
	IPKICK(true, "kick.type.ip"),
	
	ALLBANLIST("banlist.type.all"),
	BANLIST("banlist.type.uuid"),
	IPBANLIST(true, "banlist.type.ip"),
	ALLMUTELIST("mutelist.type.all"),
	MUTELIST("mutelist.type.uuid"),
	IPMUTELIST(true, "mutelist.type.ip"),
	
	HISTORY("history.type.uuid"),
	IPHISTORY(true, "history.type.ip"),
	WARNS("warns.type.uuid"),
	IPWARNS(true, "warns.type.ip"),
	
	CHECK("check.type.uuid"),
	IPCHECK(true, "check.type.ip");
	
	private final String[] permissions;
	private final boolean preferIp;
	
	private CommandType(final boolean preferIp, final String...permissions) {
		this.preferIp = preferIp;
		this.permissions = permissions;
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
}
