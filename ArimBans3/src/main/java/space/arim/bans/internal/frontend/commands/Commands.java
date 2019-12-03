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
package space.arim.bans.internal.frontend.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import space.arim.bans.ArimBans;
import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.CommandType;
import space.arim.bans.api.CommandType.Category;
import space.arim.bans.api.CommandType.IpSpec;
import space.arim.bans.api.CommandType.SubCategory;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.Subject.SubjectType;
import space.arim.bans.api.exception.ConflictingPunishmentException;
import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.api.exception.MissingPunishmentException;
import space.arim.bans.api.exception.NoGeoIpException;
import space.arim.bans.api.exception.PlayerNotFoundException;
import space.arim.bans.api.util.GeoIpInfo;
import space.arim.bans.api.util.ToolsUtil;

public class Commands implements CommandsMaster {
	
	private final ArimBans center;
	
	private static final Comparator<Punishment> DATE_COMPARATOR = new Comparator<Punishment>() {
		@Override
		public int compare(Punishment p1, Punishment p2) {
			return (int) (p1.date() - p2.date());
		}
	};
	
	private String base_perm_msg;
	private final ConcurrentHashMap<IpSpec, String> invalid = new ConcurrentHashMap<IpSpec, String>();
	private String base_usage;
	private static final String basePerm = "arimbans.commands";
	private String ip_selector_message;
	private String ip_selector_element;
	private boolean permit_blank_reason = false;
	private String default_reason;
	
	private final ConcurrentHashMap<CommandType, String> usage = new ConcurrentHashMap<CommandType, String>();
	private final ConcurrentHashMap<CommandType, String> perm = new ConcurrentHashMap<CommandType, String>();
	
	// Maps related to punishment additions.
	private final ConcurrentHashMap<PunishmentType, String> permTime = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, List<String>> durations = new ConcurrentHashMap<PunishmentType, List<String>>();
	private final ConcurrentHashMap<PunishmentType, String> exempt = new ConcurrentHashMap<PunishmentType, String>();
	private String additions_bans_error_conflicting;
	private String additions_mutes_error_conflicting;
	private final ConcurrentHashMap<SubCategory, String> successful = new ConcurrentHashMap<SubCategory, String>();
	
	private final ConcurrentHashMap<PunishmentType, String> notfound = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, Boolean> confirmUnpunish = new ConcurrentHashMap<PunishmentType, Boolean>();
	private final ConcurrentHashMap<PunishmentType, String> confirmUnpunishMsg = new ConcurrentHashMap<PunishmentType, String>();
	
	private final ConcurrentHashMap<SubCategory, Integer> perPage = new ConcurrentHashMap<SubCategory, Integer>();
	private final ConcurrentHashMap<SubCategory, String> maxPage = new ConcurrentHashMap<SubCategory, String>();
	private final ConcurrentHashMap<SubCategory, String> noPage = new ConcurrentHashMap<SubCategory, String>();
	private final ConcurrentHashMap<SubCategory, List<String>> header = new ConcurrentHashMap<SubCategory, List<String>>();
	private final ConcurrentHashMap<SubCategory, List<String>> body = new ConcurrentHashMap<SubCategory, List<String>>();
	private final ConcurrentHashMap<SubCategory, List<String>> footer = new ConcurrentHashMap<SubCategory, List<String>>();
	
	private final ConcurrentHashMap<String, String> other_status_layout = new ConcurrentHashMap<String, String>();
	private List<String> other_status_layout_body;
	
	private List<String> other_ips_layout_body;
	private String other_ips_layout_element;
	private String other_ips_error_notfound;
	private List<String> other_geoip_layout;
	private String other_geoip_error_notfound;
	private List<String> other_alts_layout_body;
	private String other_alts_layout_element;
	private String other_alts_error_notfound;
	
	private String other_reload_successful;
	
	private boolean rollback_detail;
	private String other_rollback_error_notfound;
	private String other_rollback_error_invalidnumber;
	private String other_rollback_successful_detail;
	private String other_rollback_successful_message;
	
	public Commands(ArimBans center) {
		this.center = center;
		String invalid_string = ArimBansLibrary.INVALID_STRING_CODE;
		List<String> invalid_strings = Arrays.asList(invalid_string);
		base_perm_msg = invalid_string;
		base_usage = invalid_string;
		ip_selector_message = invalid_string;
		ip_selector_element = invalid_string;
		default_reason = invalid_string;
		additions_bans_error_conflicting = invalid_string;
		additions_mutes_error_conflicting = invalid_string;
		other_status_layout_body = invalid_strings;
		other_ips_layout_body = invalid_strings;
		other_ips_layout_element = invalid_string;
		other_geoip_layout = invalid_strings;
		other_alts_layout_body = invalid_strings;
		other_alts_layout_element = invalid_string;
		for (PunishmentType pun : PunishmentType.values()) {
			notfound.put(pun, invalid_string);
			permTime.put(pun, invalid_string);
			durations.put(pun, invalid_strings);
			exempt.put(pun, invalid_string);
			confirmUnpunish.put(pun, true);
			confirmUnpunishMsg.put(pun, invalid_string);
		}
		for (SubCategory cat : SubCategory.values()) {
			successful.put(cat, invalid_string);
		}
		for (CommandType cmd : CommandType.values()) {
			usage.put(cmd, invalid_string);
			perm.put(cmd, invalid_string);
			perPage.put(cmd.subCategory(), 0);
			maxPage.put(cmd.subCategory(), invalid_string);
			noPage.put(cmd.subCategory(), invalid_string);
			header.put(cmd.subCategory(), invalid_strings);
			body.put(cmd.subCategory(), invalid_strings);
			footer.put(cmd.subCategory(), invalid_strings);
		}
	}
	
	private int parseNumber(String input, int defaultNumber) {
		try {
			int page = defaultNumber;
			page = Integer.parseInt(input);
			return (page <= 0) ? defaultNumber : page;
		} catch (NumberFormatException ex) {
			return defaultNumber;
		}
	}
	
	private boolean checkPermBase(Subject subject) {
		if (!center.subjects().hasPermission(subject, basePerm)) {
			noPermission(subject);
			return false;
		}
		return true;
	}
	
	private boolean checkPermission(Subject subject, CommandType command, boolean checkBase) {
		if (checkBase) {
			if (!checkPermBase(subject)) {
				return false;
			}
		}
		if (!center.subjects().hasPermission(subject, command.getPermission())) {
			noPermission(subject, command);
			return false;
		}
		return true;
	}
	
	private boolean checkPermission(Subject subject, CommandType command) {
		return checkPermission(subject, command, true);
	}
	
	private String keyString(Category category) {
		switch (category) {
		case ADD:
			return "additions.";
		case REMOVE:
			return "removals.";
		case LIST:
			return "list.";
		case OTHER:
			return "other.";
		default:
			throw new InternalStateException("What other category is there?!?");
		}
	}
	
	private String subKeyString(SubCategory category) {
		switch (category) {
		case BAN:
		case UNBAN:
			return "bans.";
		case MUTE:
		case UNMUTE:
			return "mutes.";
		case WARN:
		case UNWARN:
			return "warns.";
		case KICK:
			return "kicks.";
		case BANLIST:
			return "banlist.";
		case MUTELIST:
			return "mutelist.";
		case HISTORY:
			return "history.";
		case WARNS:
			return "warns.";
		case BLAME:
			return "blame.";
		case STATUS:
			return "status.";
		default:
			return category.name();
		}
	}
	
	private String keyString(SubCategory category) {
		return keyString(category.category()) + subKeyString(category);
	}
	
	private String keyString(CommandType command) {
		return keyString(command.subCategory());
	}
	
	private String keyPerm(CommandType command) {
		String keyBase = keyString(command) + "permission.";
		switch (command.ipSpec()) {
		case BOTH:
		case UUID:
			return keyBase + "command";
		case IP:
			return keyBase + "no-ip";
		default:
			throw new InternalStateException("What other ipSpec is there?!?");
		}
	}
	
	@Override
	public void execute(Subject subject, String[] rawArgs) {
		try {
			CommandType command = parseCommand(rawArgs[0]);
			if (!checkPermission(subject, command)) {
				return;
			}
			execute(subject, command, (rawArgs.length > 1) ? ToolsUtil.chopOffOne(rawArgs) : new String[] {});
		} catch (IllegalArgumentException ex) {
			usage(subject);
		}
	}
	
	@Override
	public void execute(Subject subject, CommandType command, String[] args) {
		exec(subject, command, args);
	}
	
	private String concat(String[] args) {
		StringBuilder builder = new StringBuilder(args[0]);
		for (int n = 1; n < args.length; n++) {
			builder.append(args[n]);
		}
		return builder.toString();
	}
	
	// Should only be called for targets with SubjectType == SubjectType.PLAYER
	private void ipSelector(Subject operator, Subject target, CommandType command, String[] args) {
		String base = getCmdBaseString(command) + " ";
		String extra = concat(args);
		List<String> ips = center.resolver().getIps(target.getUUID());
		StringBuilder builder = new StringBuilder();
		for (String ip : ips) {
			builder.append(ip_selector_element.replace("%IP%", ip).replace("%CMD%", base + ip + extra));
		}
		String list = builder.toString();
		center.subjects().sendMessage(operator, ip_selector_message.replace("%TARGET%", center.formats().formatSubject(target)).replace("%LIST%", list));
	}
	
	private CommandType alternateIpSpec(CommandType command) {
		for (CommandType alt : CommandType.values()) {
			if (alt.subCategory().equals(command.subCategory()) && alt.ipSpec().equals(IpSpec.IP)) {
				return alt;
			}
		}
		throw new InternalStateException("Could not find command with IpSpec.IP for subcategory " + command.subCategory());
	}
	
	private PunishmentType applicableType(CommandType command) {
		switch (command.subCategory()) {
		case BAN:
		case UNBAN:
			return PunishmentType.BAN;
		case MUTE:
		case UNMUTE:
			return PunishmentType.MUTE;
		case WARN:
		case UNWARN:
			return PunishmentType.WARN;
		case KICK:
			return PunishmentType.KICK;
		case BANLIST:
		case MUTELIST:
		case HISTORY:
		case WARNS:
		case STATUS:
		default:
			throw new InternalStateException("You called a bad method :/");
		}
	}
	
	private void exec(Subject operator, CommandType command, String[] args) {
		if (!checkPermission(operator, command)) {
			return;
		}
		if (command.canHaveNoTarget()) {
			if (command.subCategory().equals(SubCategory.RELOAD)) {
				reloadCmd(operator);
				return;
			}
			listCmd(operator, null, command, (args.length == 0) ? 1 : parseNumber(args[0], 1));
		} else {
			if (args.length <= 0) {
				usage(operator, command);
				return;
			}
			Subject target = null;
			try {
				target = center.subjects().parseSubject(args[0], false);
			} catch (IllegalArgumentException ex) {}
			try {
				target = center.subjects().parseSubject(center.resolver().resolveName(args[0]));
			} catch (PlayerNotFoundException ex) {}
			if (target == null) {
				center.subjects().sendMessage(operator, invalid.get(command.ipSpec()).replace("%TARGET%", args[0]));
				return;
			}
			switch (command.ipSpec()) {
			case UUID:
				if (!target.getType().equals(SubjectType.PLAYER)) {
					center.subjects().sendMessage(operator, invalid.get(command.ipSpec()).replace("%TARGET%", args[0]));
					return;
				}
				break;
			case IP:
				if (target.getType().equals(SubjectType.PLAYER)) {
					ipSelector(operator, target, command, args);
					return;
				}
			case BOTH:
				if (!target.getType().equals(SubjectType.PLAYER)) {
					if (!checkPermission(operator, alternateIpSpec(command), false)) {
						return;
					}
				}
			default:
				break;
			}
			args = ToolsUtil.chopOffOne(args);
			if (command.category().equals(Category.ADD)) {
				punCmd(operator, target, command, args);
			} else if (command.category().equals(Category.REMOVE)) {
				unpunCmd(operator, target, command, args);
			} else if (command.category().equals(Category.LIST)) {
				listCmd(operator, target, command, (args.length == 0) ? 1 : parseNumber(args[0], 1));
			} else if (command.category().equals(Category.OTHER)) {
				otherCmd(operator, target, command, args);
			}
		}
	}
	
	private void punCmd(Subject operator, Subject target, CommandType command, String[] args) {
		if (args.length == 0) {
			usage(operator, command);
			return;
		}
		PunishmentType type = applicableType(command);
		long span;
		if (!type.equals(PunishmentType.KICK)) {
			long max = 0;
			List<String> durPerms = durations.get(type);
			for (String perm : durPerms) {
				long permTime = center.formats().toMillis(perm);
				if (permTime > max) {
					if (center.subjects().hasPermission(operator, perm)) {
						max = permTime;
					}
				}
			}
			span = center.formats().toMillis(args[0]);
			if (span == 0) {
				span = -1L;
			} else {
				args = ToolsUtil.chopOffOne(args);
			}
			if (span == -1L && max != -1L || span > max) {
				center.subjects().sendMessage(operator, permTime.get(type));
				return;
			}
		} else {
			span = -1L;
		}
		String reason;
		if (args.length > 0) {
			reason = concat(args);
		} else if (permit_blank_reason) {
			reason = default_reason;
		} else {
			usage(operator, command);
			return;
		}
		Punishment punishment = new Punishment(type, target, operator, reason, (span == -1L) ? span : span + System.currentTimeMillis());
		try {
			center.punishments().addPunishments(false, punishment);
			center.subjects().sendMessage(operator, center.formats().formatMessageWithPunishment(successful.get(command.subCategory()), punishment));
			center.subjects().sendNotif(punishment, true, operator);
			center.environment().enforcer().enforce(punishment, center.formats().useJson());
		} catch (ConflictingPunishmentException ex) {
			String conflict = (punishment.type().equals(PunishmentType.BAN)) ? additions_bans_error_conflicting : additions_mutes_error_conflicting;
			center.subjects().sendMessage(operator, conflict.replace("%TARGET%", center.formats().formatSubject(punishment.subject())));
		}
	}
	
	private void unpunCmd(Subject operator, Subject target, CommandType command, String[] args) {
		PunishmentType type = applicableType(command);
		if (command.subCategory().equals(SubCategory.UNWARN)) {
			if (args.length > 0) {
				if (args[0].equals("internal_confirm")) {
					try {
						long date = Long.parseLong(args[1]);
						Set<Punishment> active = center.punishments().getAllPunishments();
						for (Punishment punishment : active) {
							if (punishment.date() == date) {
								center.punishments().removePunishments(false, punishment);
								center.subjects().sendMessage(operator, center.formats().formatMessageWithPunishment(successful.get(command.subCategory()), punishment));
								center.subjects().sendNotif(punishment, false, operator);
								return;
							}
						}
					} catch (NumberFormatException ex) {}
				} else {
					try {
						int id = (Integer.parseInt(args[0]) - 1);
						ArrayList<Punishment> applicable = new ArrayList<Punishment>(center.punishments().getPunishments(target));
						applicable.sort(DATE_COMPARATOR);
						if (id < 0 || id >= applicable.size()) {
							center.subjects().sendMessage(operator, notfound.get(type).replace("%NUMBER%", args[0]).replace("%TARGET%", center.formats().formatSubject(target)));
							return;
						}
						Punishment punishment = applicable.get(id);
						if (confirmUnpunish.get(type)) {
							String cmd = getCmdBaseString(command) + " internal_confirm " + punishment.date();
							center.subjects().sendMessage(operator, center.formats().formatMessageWithPunishment(confirmUnpunishMsg.get(type), punishment).replace("%CMD%", cmd));
							return;
						}
						center.punishments().removePunishments(false, punishment);
						center.subjects().sendMessage(operator, center.formats().formatMessageWithPunishment(successful.get(command.subCategory()), punishment));
						center.subjects().sendNotif(punishment, false, operator);
						return;
					} catch (NumberFormatException ex) {}
				}
			}
			usage(operator, command);
			return;
		}
		Punishment punishment;
		try {
			punishment = center.punishments().getPunishment(target, type);
		} catch (MissingPunishmentException ex) {
			center.subjects().sendMessage(operator, notfound.get(type).replace("%TARGET%", center.formats().formatSubject(target)));
			return;
		}
		if (confirmUnpunish.get(type) && !args[0].equals("internal_confirm")) {
			String cmd = getCmdBaseString(command) + " internal_confirm";
			center.subjects().sendMessage(operator, center.formats().formatMessageWithPunishment(confirmUnpunishMsg.get(type), punishment).replace("%CMD%", cmd));
		} else {
			center.punishments().removePunishments(false, punishment);
			center.subjects().sendMessage(operator, center.formats().formatMessageWithPunishment(successful.get(command.subCategory()), punishment));
			center.subjects().sendNotif(punishment, false, operator);
		}
	}
	
	private Set<Punishment> getAllForListParams(Subject target, CommandType command) {
		switch (command.subCategory()) {
		case BANLIST:
			return center.punishments().getAllPunishments(PunishmentType.BAN);
		case MUTELIST:
			return center.punishments().getAllPunishments(PunishmentType.MUTE);
		case HISTORY:
			return center.punishments().getHistory(target);
		case WARNS:
			return center.punishments().getPunishments(target, PunishmentType.WARN);
		case BLAME:
			return center.punishments().getAllPunishments();
		default:
			return center.punishments().getPunishments(target);
		}
	}
	
	private String getCmdBaseString(CommandType command) {
		return "arimbans " + command.name();
	}
	
	private void reloadCmd(Subject operator) {
		center.refresh(false);
		center.subjects().sendMessage(operator, other_reload_successful);
	}
	
	private void listCmd(Subject operator, Subject target, CommandType command, int page) {
		Set<Punishment> applicable = getAllForListParams(target, command);
		list(operator, new Lister<Punishment>(page, perPage.get(command.subCategory()), maxPage.get(command.subCategory()), noPage.get(command.subCategory()), applicable, header.get(command.subCategory()), body.get(command.subCategory()), footer.get(command.subCategory())) {
			@Override
			boolean check(Punishment punishment) {
				switch (command.subCategory()) {
				case BANLIST: // falls through to next sub-block
				case MUTELIST:
					if (command.ipSpec().equals(IpSpec.UUID)) {
						return punishment.subject().getType().equals(Subject.SubjectType.PLAYER);
					} else if (command.ipSpec().equals(IpSpec.IP)) {
						return punishment.subject().getType().equals(Subject.SubjectType.IP);
					}
				case BLAME:
					return punishment.subject().compare(target);
				default:
					return true;
				}
			}
			@Override
			String getNextPageCmd() {
				int nextPage = page + 1;
				return getCmdBaseString(command) + " " + nextPage;
			}
		});
	}
	
	private void otherCmd(Subject operator, Subject target, CommandType command, String[] extraArgs) {
		switch (command.subCategory()) {
		case STATUS:
			statusCmd(operator, target);
			break;
		case IPS:
			ipsCmd(operator, target);
			break;
		case GEOIP:
			geoipCmd(operator, target);
			break;
		case ALTS:
			altsCmd(operator, target);
			break;
		case ROLLBACK:
			rollbackCmd(operator, target, (extraArgs.length > 0) ? extraArgs[0] : "all");
			break;
		default:
			throw new InternalStateException("Wrong command execution method!");
		}
	}
	
	private void statusCmd(Subject operator, Subject target) {
		boolean banned = center.punishments().hasPunishment(target, PunishmentType.BAN);
		boolean muted = center.punishments().hasPunishment(target, PunishmentType.MUTE);
		int warns = center.punishments().getPunishments(target, PunishmentType.WARN).size();
		String header;
		String info;
		if (target.getType().equals(SubjectType.PLAYER)) {
			header = other_status_layout.get("player.header");
			info = other_status_layout.get("player.info").replace("%IPS_CMD%", "arimbans ips " + target.getUUID());
		} else if (target.getType().equals(SubjectType.IP)) {
			header = other_status_layout.get("ip.header");
			info = other_status_layout.get("ip.info").replace("%GEOIP_CMD%", "arimbans geoip " + target.getIP()).replace("%ALTS_CMD%", "arimbans alts " + target.getIP());
		} else {
			throw new InternalStateException("Target cannot be the console!");
		}
		String banStatusKey = (banned) ? "ban-status.banned" : "ban-status.not-banned";
		String muteStatusKey = (muted) ? "mute-status.muted" : "mute-status.not-muted";
		String warnStatusKey = (warns == 0) ? "warn-status.no-warns" : "warn-status.warn-count";
		String banStatus = other_status_layout.get(banStatusKey);
		String muteStatus = other_status_layout.get(muteStatusKey);
		String warnStatus = other_status_layout.get(warnStatusKey);
		String[] msgs = other_status_layout_body.toArray(new String[0]);
		for (int n = 0; n < msgs.length; n++) {
			msgs[n] = msgs[n].replace("%HEADER%", header).replace("%INFO%", info).replace("%BAN_STATUS%", banStatus).replace("%MUTE_STATUS%", muteStatus).replace("%WARN_STATUS%", warnStatus);
		}
		center.subjects().sendMessage(operator, msgs);
	}
	
	private void ipsCmd(Subject operator, Subject target) {
		String[] msgs = other_ips_layout_body.toArray(new String[0]);
		String targetDisplay = center.formats().formatSubject(target);
		List<String> ips = center.resolver().getIps(target.getUUID());
		if (ips.isEmpty()) {
			center.subjects().sendMessage(operator, other_ips_error_notfound.replace("%TARGET%", targetDisplay));
			return;
		}
		StringBuilder builder = new StringBuilder();
		for (String ip : ips) {
			builder.append(other_ips_layout_element.replace("%IP%", ip).replace("%GEOIP_CMD%", "arimbans geoip " + ip).replace("%STATUS_CMD%", "arimbans status " + ip));
		}
		for (int n = 0; n < msgs.length; n++) {
			msgs[n] = msgs[n].replace("%TARGET%", targetDisplay).replace("%LIST%", builder.toString());
		}
		center.subjects().sendMessage(operator, msgs);
	}
	
	private void geoipCmd(Subject operator, Subject target) {
		String[] msgs = other_geoip_layout.toArray(new String[0]);
		GeoIpInfo geoip;
		String targetDisplay = center.formats().formatSubject(target);
		try {
			geoip = center.resolver().lookupIp(target.getIP());
		} catch (NoGeoIpException ex) {
			center.subjects().sendMessage(operator, other_geoip_error_notfound.replace("%TARGET%", targetDisplay));
			return;
		}
		for (int n = 0; n < msgs.length; n++) {
			msgs[n] = msgs[n].replace("%TARGET%", targetDisplay).replace("%COUNTRY_CODE%", geoip.country_code).replace("%COUNTRY_NAME%", geoip.country_name).replace("%REGION_CODE%", geoip.region_code).replace("%REGION_NAME%", geoip.region_name).replace("%CITY%", geoip.city).replace("%ZIP%", geoip.zip).replace("%LONGITUDE%", Double.toString(geoip.longitude)).replace("%LATITUDE%", Double.toString(geoip.latitude));
		}
		center.subjects().sendMessage(operator, msgs);
	}
	
	private void altsCmd(Subject operator, Subject target) {
		String[] msgs = other_alts_layout_body.toArray(new String[0]);
		String targetDisplay = center.formats().formatSubject(target);
		List<UUID> players = center.resolver().getPlayers(target.getIP());
		if (players.isEmpty()) {
			center.subjects().sendMessage(operator, other_alts_error_notfound.replace("%TARGET%", center.formats().formatSubject(target)));
			return;
		}
		StringBuilder builder = new StringBuilder();
		for (UUID uuid : players) {
			String name;
			try {
				name = center.resolver().resolveUUID(uuid);
			} catch (PlayerNotFoundException ex) {
				center.logError(ex);
				name = uuid.toString();
			}
			builder.append(other_alts_layout_element.replace("%PLAYER%", name).replace("%STATUS_CMD%", "arimbans status " + uuid));
		}
		for (int n = 0; n < msgs.length; n++) {
			msgs[n] = msgs[n].replace("%TARGET%", targetDisplay).replace("%LIST%", builder.toString());
		}
		center.subjects().sendMessage(operator, msgs);
	}
	
	private void rollbackCmd(Subject operator, Subject target, String numberArg) {
		int max;
		if (!numberArg.equals("all")) {
			max = parseNumber(numberArg, 0);
			if (max == 0) {
				center.subjects().sendMessage(operator, other_rollback_error_invalidnumber.replace("%NUMBER%", numberArg));
				return;
			}
		} else {
			max = Integer.MAX_VALUE;
		}
		ArrayList<Punishment> applicable = new ArrayList<Punishment>(center.punishments().getAllPunishments());
		applicable.sort(DATE_COMPARATOR);
		int n = 0;
		for (Iterator<Punishment> it = applicable.iterator(); it.hasNext();) {
			Punishment punishment = it.next();
			if (punishment.operator().compare(operator)) {
				if (n >= max) {
					it.remove();
				} else {
					if (rollback_detail) {
						center.subjects().sendMessage(operator, center.formats().formatMessageWithPunishment(other_rollback_successful_detail, punishment));
					}
					n++;
				}
			} else {
				it.remove();
			}
		}
		if (applicable.isEmpty()) {
			center.subjects().sendMessage(operator, other_rollback_error_notfound.replace("%TARGET%", center.formats().formatSubject(target)));
			return;
		}
		center.subjects().sendMessage(operator, other_rollback_successful_message.replace("%NUMBER%", numberArg).replace("%TARGET%", center.formats().formatSubject(target)));
	}
	
	private void list(Subject operator, Lister<Punishment> lister) {
		ArrayList<Punishment> punishments = new ArrayList<Punishment>(lister.applicable);
		for (Iterator<Punishment> it = punishments.iterator(); it.hasNext();) {
			if (!lister.check(it.next())) {
				it.remove();
			}
		}
		if (punishments.isEmpty()) {
			center.subjects().sendMessage(operator, lister.noPagesMsg);
			return;
		}
		int maxPage = (punishments.size() - 1)/lister.perPage + 1;
		if (lister.page > maxPage) {
			center.subjects().sendMessage(operator, lister.maxPagesMsg.replace("%PAGE%", Integer.toString(lister.page)).replace("%MAXPAGE%", Integer.toString(maxPage)));
			return;
		}
		List<String> header = lister.header;
		List<String> body = lister.body;
		List<String> footer = lister.footer;
		for (int n = 0; n < header.size(); n++) {
			header.set(n, header.get(n).replace("%PAGE%", Integer.toString(lister.page)).replace("%MAXPAGE%", Integer.toString(maxPage)).replace("%CMD%", lister.getNextPageCmd()));
		}
		for (int n = 0; n < footer.size(); n++) {
			footer.set(n, footer.get(n).replace("%PAGE%", Integer.toString(lister.page)).replace("%MAXPAGE%", Integer.toString(maxPage)).replace("%CMD%", lister.getNextPageCmd()));
		}
		punishments.sort(DATE_COMPARATOR);
		center.subjects().sendMessage(operator, header.toArray(new String[0]));
		for (Punishment p : punishments) {
			String[] msgs = body.toArray(new String[0]);
			for (int n = 0; n < msgs.length; n++) {
				msgs[n] = center.formats().formatMessageWithPunishment(msgs[n], p).replace("%PAGE%", Integer.toString(lister.page)).replace("%MAXPAGE%", Integer.toString(maxPage)).replace("%CMD%", lister.getNextPageCmd());
			}
			center.subjects().sendMessage(operator, true, msgs);
		}
		center.subjects().sendMessage(operator, true, footer.toArray(new String[0]));
	}

	@Override
	public void usage(Subject subject) {
		if (!checkPermBase(subject)) {
			return;
		}
		center.subjects().sendMessage(subject, base_usage);
	}
	
	@Override
	public void usage(Subject subject, CommandType command) {
		if (!checkPermission(subject, command)) {
			return;
		}
		center.subjects().sendMessage(subject, usage.get(command));
	}
	
	@Override
	public void refreshConfig(boolean first) {
		permit_blank_reason = center.config().getConfigBoolean("commands.reasons.permit-blank");
		default_reason = center.config().getConfigString("commands.reasons.default-reason");
		
		String base1 = "commands.confirm-unpunish";
		for (PunishmentType type : PunishmentType.values()) {
			switch (type) {
			case BAN:
				confirmUnpunish.put(type, center.config().getConfigBoolean(base1 + "unbans"));
				break;
			case MUTE:
				confirmUnpunish.put(type, center.config().getConfigBoolean(base1 + "unmutes"));
				break;
			case WARN:
				confirmUnpunish.put(type, center.config().getConfigBoolean(base1 + "unwarns"));
			case KICK:
			default:
				break;
			}
		}
		
		rollback_detail = center.config().getConfigBoolean("commands.rollback-detail");
		
	}
	
	@Override
	public void refreshMessages(boolean first) {
		
		base_perm_msg = center.config().getMessagesString("all.base-permission-message");
		for (IpSpec spec : IpSpec.values()) {
			invalid.put(spec, center.config().getMessagesString("all.invalid." + spec.name().toLowerCase()));
		}
		base_usage = center.config().getMessagesString("all.usage");
		
		ip_selector_message = center.config().getMessagesString("all.ip-selector.message");
		ip_selector_element = center.config().getMessagesString("all.ip-selector.element");
		
		additions_bans_error_conflicting = center.config().getMessagesString("additions.bans.error.conflicting");
		additions_mutes_error_conflicting = center.config().getMessagesString("additions.mutes.error.conflicting");
		
		for (CommandType cmd : CommandType.values()) {
			usage.put(cmd, center.config().getMessagesString(keyString(cmd) + "usage"));
			perm.put(cmd, center.config().getMessagesString(keyPerm(cmd)));
		}
		
		for (SubCategory category : SubCategory.values()) {
			String leadKey = keyString(category);
			switch (category) {
			case BANLIST:
			case MUTELIST:
			case HISTORY:
			case WARNS:
			case BLAME: // all 5 cases fall-through to here
				perPage.put(category, center.config().getMessagesInt(leadKey + "per-page"));
				noPage.put(category, center.config().getMessagesString(leadKey + "no-pages"));
				maxPage.put(category, center.config().getMessagesString(leadKey + "max-pages"));
				header.put(category, center.config().getMessagesStrings(leadKey + "layout.header"));
				body.put(category, center.config().getMessagesStrings(leadKey + "layout.body"));
				footer.put(category, center.config().getMessagesStrings(leadKey + "layout.footer"));
			default:
				break;
			}
		}
		
		for (PunishmentType type : PunishmentType.values()) {
			String leadKey1 = center.config().keyString(type, Category.ADD);
			String leadKey2 = center.config().keyString(type, Category.REMOVE);
			SubCategory categoryAdd = fromPunishmentType(type, true);
			SubCategory categoryRemove = fromPunishmentType(type, false);
			switch (type) {
			case BAN: // falls through to warn case
			case MUTE: // falls through to warn case
			case WARN:
				// removals do not apply to kicks
				notfound.put(type, center.config().getMessagesString(leadKey2 + "error.not-found"));
				successful.put(categoryRemove, center.config().getMessagesString(leadKey2 + "successful.message"));
				confirmUnpunishMsg.put(type, center.config().getMessagesString(leadKey2 + "error.confirm"));
				// durations do not apply to kicks
				permTime.put(type, center.config().getMessagesString(leadKey1 + "permission.time"));
				durations.put(type, center.config().getMessagesStrings(leadKey1 + "permission.dur-perms"));
				// then falls through to kick case
			case KICK:
				exempt.put(type, center.config().getMessagesString(leadKey1 + "error.exempt"));
				successful.put(categoryAdd, center.config().getMessagesString(leadKey1 + "successful.message"));
				break;
			default:
				throw new InternalStateException("What other punishment type is there?!?");
			}
		}
		
		String[] other_status_layout_keys = {"player.header", "player.info", "ip.header", "ip.info", "ban-status.banned", "ban-status.not-banned", "mute-status.muted", "mute-status.not-muted", "warn-status.warn-count", "warn-status.no-warns"};
		for (String key : other_status_layout_keys) {
			other_status_layout.put(key, center.config().getMessagesString("other.status.layout." + key));
		}
		other_status_layout_body = center.config().getMessagesStrings("other.status.layout.body");
		
		other_ips_layout_body = center.config().getMessagesStrings("other.ips.layout.body");
		other_ips_layout_element = center.config().getMessagesString("other.ips.layout.element");
		other_ips_error_notfound = center.config().getMessagesString("other.ips.error.not-found");
		other_geoip_layout = center.config().getMessagesStrings("other.geoip.layout");
		other_geoip_error_notfound = center.config().getMessagesString("other.geoip.error.not-found");
		other_alts_layout_body = center.config().getMessagesStrings("other.alts.layout.body");
		other_alts_layout_element = center.config().getMessagesString("other.alts.layout.element");
		other_alts_error_notfound = center.config().getMessagesString("other.alts.error-not-found");
		
		other_reload_successful = center.config().getMessagesString("other.reload.successful");
		
		other_rollback_error_notfound = center.config().getMessagesString("other.rollback.error.not-found");
		other_rollback_error_invalidnumber = center.config().getMessagesString("other.rollback.error.invalid-number");
		other_rollback_successful_detail = center.config().getMessagesString("other.rollback.successful.detail");
		other_rollback_successful_message = center.config().getMessagesString("other.rollback.successful.message");
		
	}
	
	// Exact same method as in FormatsMaster implementation
	private SubCategory fromPunishmentType(PunishmentType type, boolean add) {
		switch (type) {
		case BAN:
			return (add) ? SubCategory.BAN : SubCategory.UNBAN;
		case MUTE:
			return (add) ? SubCategory.MUTE : SubCategory.UNMUTE;
		case WARN:
			return (add) ? SubCategory.WARN : SubCategory.UNWARN;
		case KICK:
			return SubCategory.KICK;
		default:
			throw new InternalStateException("What other punishment type is there?!?");
		}
	}
	
	@Override
	public void noPermission(Subject subject) {
		center.subjects().sendMessage(subject, base_perm_msg);
	}

	@Override
	public void noPermission(Subject subject, CommandType command) {
		center.subjects().sendMessage(subject, perm.get(command));
	}
	
}

abstract class Lister<T> {
	
	final int page;
	final int perPage;
	final String maxPagesMsg;
	final String noPagesMsg;
	final Set<T> applicable;
	final List<String> header;
	final List<String> body;
	final List<String> footer;
	
	Lister(int page, int perPage, String maxPagesMsg, String noPagesMsg, Set<T> applicable, List<String> header, List<String> body, List<String> footer) {
		this.page = page;
		this.perPage = perPage;
		this.maxPagesMsg = maxPagesMsg;
		this.noPagesMsg = noPagesMsg;
		this.applicable = applicable;
		this.header = header;
		this.body = body;
		this.footer = footer;
	}
	
	abstract boolean check(T object);
	
	abstract String getNextPageCmd();
	
}
