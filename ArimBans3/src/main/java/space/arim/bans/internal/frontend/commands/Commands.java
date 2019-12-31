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
import java.util.function.Predicate;
import java.util.logging.Level;

import space.arim.bans.ArimBans;
import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.CommandType;
import space.arim.bans.api.CommandType.IpSpec;
import space.arim.bans.api.CommandType.SubCategory;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.Subject.SubjectType;
import space.arim.bans.api.exception.ConflictingPunishmentException;
import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.api.exception.MissingCacheException;
import space.arim.bans.api.exception.MissingPunishmentException;
import space.arim.bans.api.exception.NoGeoIpException;
import space.arim.bans.api.exception.TypeParseException;

import space.arim.universal.util.UniversalUtil;
import space.arim.universal.util.collections.CollectionsUtil;

import space.arim.api.framework.PlayerNotFoundException;
import space.arim.api.util.StringsUtil;
import space.arim.api.util.web.GeoIpInfo;

public class Commands implements CommandsMaster {
	
	private final ArimBans center;
	
	private static final Comparator<Punishment> DATE_LATEST_FIRST_COMPARATOR = (p1, p2) -> (int) (p1.date() - p2.date());
	
	private static final String ROLLBACK_ALL_ARG = "all";
	
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
	private final ConcurrentHashMap<PunishmentType, String> noSilent = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, String> noPassive = new ConcurrentHashMap<PunishmentType, String>();
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
	
	private String other_editreason_error_notfound;
	
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
			noSilent.put(pun, invalid_string);
			noPassive.put(pun, invalid_string);
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
	
	private String keyString(CommandType.Category category) {
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
		case IPS:
			return "ips.";
		case ALTS:
			return "alts.";
		case ROLLBACK:
			return "rollback.";
		case EDITREASON:
			return "editreason.";
		case RELOAD:
			return "reload.";
		default:
			return category.name().toLowerCase() + ".";
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
	
	@Override
	public void execute(Subject subject, String[] rawArgs) {
		try {
			CommandType command = CommandType.parseCommand(rawArgs[0]);
			if (!checkPermission(subject, command)) {
				return;
			}
			execute(subject, command, StringsUtil.chopOffOne(rawArgs));
		} catch (TypeParseException ex) {
			usage(subject);
		}
	}
	
	@Override
	public void execute(Subject subject, CommandType command, String[] args) {
		if (command.requiresAsynchronisation() && !UniversalUtil.get().isAsynchronous()) {
			center.async(() -> exec(subject, command, args));
		} else {
			exec(subject, command, args);
		}
	}
	
	private void exec(Subject operator, CommandType command, String[] args) {
		center.logs().log(Level.FINE, "Executing command " + command + " for operator " + operator + " with args " + StringsUtil.concat(args, ','));
		if (!checkPermission(operator, command)) {
			return;
		}
		if (command.canHaveNoTarget()) {
			switch (command.subCategory()) {
			case RELOAD:
				reloadCmd(operator);
				break;
			case EDITREASON:
				editReasonCmd(operator, args);
				break;
			default:
				listCmd(operator, null, command, args.length == 0 ? 1 : parseNumber(args[0], 1));
				break;
			}
			return;
		}
		if (args.length <= 0) {
			usage(operator, command);
			return;
		}
		Subject target = null;
		try {
			target = center.subjects().parseSubject(args[0], false);
		} catch (IllegalArgumentException ex) {}
		try {
			target = center.subjects().parseSubject(center.resolver().resolveName(args[0], center.environment().isOnlineMode()));
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
			break;
		case BOTH:
			if (!target.getType().equals(SubjectType.PLAYER) && !checkPermission(operator, command.alternateIpSpec(), false)) {
				return;
			}
			break;
		default:
			break;
		}
		args = StringsUtil.chopOffOne(args);
		if (command.category().equals(CommandType.Category.ADD)) {
			punCmd(operator, target, command, args);
		} else if (command.category().equals(CommandType.Category.REMOVE)) {
			unpunCmd(operator, target, command, args);
		} else if (command.category().equals(CommandType.Category.LIST)) {
			listCmd(operator, target, command, (args.length == 0) ? 1 : parseNumber(args[0], 1));
		} else if (command.category().equals(CommandType.Category.OTHER)) {
			otherCmd(operator, target, command, args);
		}
	}
	
	// Should only be called for targets with SubjectType == SubjectType.PLAYER
	private void ipSelector(Subject operator, Subject target, CommandType command, String[] args) {
		String base = getCmdBaseString(command) + " ";
		String extra = StringsUtil.concat(args, ' ');
		List<String> ips;
		try {
			ips = center.resolver().getIps(target.getUUID());
		} catch (MissingCacheException ex) {
			center.subjects().sendMessage(operator, invalid.get(IpSpec.IP).replace("%TARGET%", center.formats().formatSubject(target)));
			return;
		}
		StringBuilder builder = new StringBuilder();
		for (String ip : ips) {
			builder.append(ip_selector_element.replace("%IP%", ip).replace("%CMD%", base + ip + extra));
		}
		String list = builder.toString();
		center.subjects().sendMessage(operator, ip_selector_message.replace("%TARGET%", center.formats().formatSubject(target)).replace("%LIST%", list));
	}
	
	private void punCmd(Subject operator, Subject target, CommandType command, String[] args) {
		if (args.length == 0) {
			usage(operator, command);
			return;
		}
		PunishmentType type = applicableType(command);
		if (center.subjects().hasPermission(target, "arimbans." + type.name().toLowerCase() + ".exempt") && !center.subjects().hasPermission(operator, "arimbans." + type.name().toLowerCase() + ".exempt.bypass")) {
			center.subjects().sendMessage(operator, exempt.get(type).replace("%TARGET%", center.formats().formatSubject(target)));
			return;
		}
		long span = -1L;
		if (!type.equals(PunishmentType.KICK)) {
			long max = 0;
			String basePerm = "arimbans." + type.name().toLowerCase() + ".dur.";
			List<String> durPerms = durations.get(type);
			if (center.subjects().hasPermission(operator, basePerm + "perm")) {
				max = -1L;
			} else {
				for (String timePerm : durPerms) {
					long permTime = center.formats().toMillis(timePerm);
					if (permTime > max) {
						if (center.subjects().hasPermission(operator, basePerm + timePerm)) {
							max = permTime;
						}
					}
				}
			}
			span = center.formats().toMillis(args[0]);
			if (span == 0) {
				span = -1L;
			} else {
				args = StringsUtil.chopOffOne(args);
			}
			if (span == -1L && max != -1L || span > max) {
				center.subjects().sendMessage(operator, permTime.get(type).replace("%MAXTIME%", center.formats().formatTime(max, false)));
				return;
			}
		}
		String reason = "";
		boolean silent = false;
		boolean passive = false;
		if (args.length > 0) {
			for (String arg : args) {
				if (arg.startsWith("-")) {
					if (arg.contains("s")) {
						if (!center.subjects().hasPermission(operator, "arimbans." + type.name().toLowerCase() + ".passive")) {
							center.subjects().sendMessage(operator, noSilent.get(type));
							return;
						}
						silent = true;
					}
					if (arg.contains("p")) {
						if (!center.subjects().hasPermission(operator, "arimbans." + type.name().toLowerCase() + ".passive")) {
							center.subjects().sendMessage(operator, noPassive.get(type));
							return;
						}
						passive = true;
					}
					if (silent || passive) {
						arg = "";
					}
				}
			}
			reason = StringsUtil.concat(args, ' ');
		}
		if (reason.isEmpty() && permit_blank_reason) {
			reason = default_reason;
		} else if (reason.isEmpty()) {
			usage(operator, command);
			return;
		}
		Punishment punishment = new Punishment(center.getNextAvailablePunishmentId(), type, target, operator, reason, (span == -1L) ? span : span + System.currentTimeMillis(), silent, passive);
		try {
			center.punishments().addPunishments(punishment);
			enact(operator, command, punishment, true);
		} catch (ConflictingPunishmentException ex) {
			String conflict = (punishment.type().equals(PunishmentType.BAN)) ? additions_bans_error_conflicting : additions_mutes_error_conflicting;
			center.subjects().sendMessage(operator, conflict.replace("%TARGET%", center.formats().formatSubject(punishment.subject())));
		}
	}
	
	private void enact(Subject operator, CommandType command, Punishment punishment, boolean add) {
		center.subjects().sendMessage(operator, center.formats().formatMessageWithPunishment(successful.get(command.subCategory()), punishment));
		center.corresponder().enact(punishment, add, operator);
	}
	
	private void unpunCmd(Subject operator, Subject target, CommandType command, String[] args) {
		boolean silent = false;
		for (String arg : args) {
			if (arg.startsWith("-")) {
				if (arg.contains("s")) {
					silent = true;
				}
			}
		}
		PunishmentType type = applicableType(command);
		if (command.subCategory().equals(SubCategory.UNWARN)) {
			if (args.length > 0) {
				if (args[0].equals("internal_confirm")) {
					try {
						Punishment punishment = center.corresponder().getPunishmentById(Integer.parseInt(args[1]));
						punishment.setSilent(silent);
						center.punishments().removePunishments(punishment);
						enact(operator, command, punishment, false);
						return;
					} catch (MissingPunishmentException ex) {
						center.subjects().sendMessage(operator, notfound.get(type).replace("%NUMBER%", args[0]).replace("%TARGET%", center.formats().formatSubject(target)));
						return;
					} catch (NumberFormatException ignored) {}
				} else {
					try {
						Punishment punishment = center.corresponder().getPunishmentById(Integer.parseInt(args[0]));
						if (confirmUnpunish.get(type)) {
							String cmd = getCmdBaseString(command) + " internal_confirm " + punishment.id() + " " + StringsUtil.concat(args, ' ');
							center.subjects().sendMessage(operator, center.formats().formatMessageWithPunishment(confirmUnpunishMsg.get(type), punishment).replace("%CMD%", cmd));
							return;
						}
						punishment.setSilent(silent);
						center.punishments().removePunishments(punishment);
						enact(operator, command, punishment, false);
						return;
					} catch (MissingPunishmentException ex) {
						center.subjects().sendMessage(operator, notfound.get(type).replace("%NUMBER%", args[0]).replace("%TARGET%", center.formats().formatSubject(target)));
						return;
					} catch (NumberFormatException ignored) {}
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
			String cmd = getCmdBaseString(command) + " internal_confirm " + StringsUtil.concat(args, ' ');
			center.subjects().sendMessage(operator, center.formats().formatMessageWithPunishment(confirmUnpunishMsg.get(type), punishment).replace("%CMD%", cmd));
		} else {
			try {
				punishment.setSilent(silent);
				center.punishments().removePunishments(punishment);
				enact(operator, command, punishment, false);
			} catch (MissingPunishmentException ex) {
				center.subjects().sendMessage(operator, notfound.get(type).replace("%TARGET%", center.formats().formatSubject(target)));
				return;
			}
		}
	}
	
	private String getCmdBaseString(CommandType command) {
		return "arimbans " + command.name();
	}
	
	private void reloadCmd(Subject operator) {
		center.refresh(false);
		center.subjects().sendMessage(operator, successful.get(SubCategory.RELOAD));
	}
	
	private void editReasonCmd(Subject operator, String[] args) {
		if (args.length > 1) {
			try {
				Punishment punishment = center.corresponder().getPunishmentById(Integer.parseInt(args[0]));
				String reason = StringsUtil.concatRange(args, ' ', 1, args.length);
				center.punishments().changeReason(punishment, reason);
				center.subjects().sendMessage(operator, successful.get(SubCategory.EDITREASON).replace("%ID%", args[0]).replace("%REASON%", reason));
				return;
			} catch (MissingPunishmentException ex) {
				center.subjects().sendMessage(operator, other_editreason_error_notfound.replace("%ID%", args[0]));
				return;
			} catch (NumberFormatException ignored) {}
		}
		usage(operator, CommandType.EDITREASON);
	}
	
	private String nextPageCmd(CommandType command, int page) {
		int nextPage = page + 1;
		return getCmdBaseString(command) + " " + nextPage;
	}
	
	private Predicate<Punishment> getListFilter(CommandType command, Subject target) {
		switch (command.subCategory()) {
		case BANLIST:
			return (punishment) -> !punishment.type().equals(PunishmentType.BAN) || command.ipSpec().equals(IpSpec.UUID) && !punishment.subject().getType().equals(Subject.SubjectType.PLAYER) || command.ipSpec().equals(IpSpec.IP) && !punishment.subject().getType().equals(Subject.SubjectType.IP);
		case MUTELIST:
			return (punishment) -> !punishment.type().equals(PunishmentType.MUTE) || command.ipSpec().equals(IpSpec.UUID) && !punishment.subject().getType().equals(Subject.SubjectType.PLAYER) || command.ipSpec().equals(IpSpec.IP) && !punishment.subject().getType().equals(Subject.SubjectType.IP);
		case WARNS:
			return (punishment) -> !punishment.subject().equals(target) || !punishment.type().equals(PunishmentType.WARN);
		case HISTORY:
			return (punishment) -> !punishment.subject().equals(target);
		case BLAME:
			return (punishment) -> !punishment.operator().equals(target);
		default:
			return (punishment) -> false;
		}
	}
	
	private static String replaceListVars(String input, int page, int maxPage, String nextPageCmd) {
		return input.replace("%PAGE%", Integer.toString(page)).replace("%MAXPAGE%", Integer.toString(maxPage)).replace("%CMD%", nextPageCmd);
	}
	
	private void listCmd(Subject operator, Subject target, CommandType command, int page) {
		List<Punishment> punishments = new ArrayList<Punishment>(SubCategory.HISTORY.equals(command.subCategory()) ? center.punishments().getHistoryCopy() : center.punishments().getActiveCopy());
		punishments.removeIf(getListFilter(command, target));
		if (punishments.isEmpty()) {
			center.subjects().sendMessage(operator, noPage.get(command.subCategory()));
			return;
		}
		String maxPagesMsg = maxPage.get(command.subCategory());
		int maxPage = (punishments.size() - 1)/perPage.get(command.subCategory()) + 1;
		if (page > maxPage) {
			center.subjects().sendMessage(operator, maxPagesMsg.replace("%PAGE%", Integer.toString(page)).replace("%MAXPAGE%", Integer.toString(maxPage)));
			return;
		}
		List<String> headerList = header.get(command.subCategory());
		List<String> bodyList = body.get(command.subCategory());
		List<String> footerList = footer.get(command.subCategory());
		String nextPageCmd = nextPageCmd(command, page);
		punishments.sort(DATE_LATEST_FIRST_COMPARATOR);
		center.subjects().sendMessage(operator, false, CollectionsUtil.wrapAll(headerList.toArray(new String[] {}), (headerItem) -> replaceListVars(headerItem, page, maxPage, nextPageCmd)));
		punishments.forEach((punishment) -> {
			center.subjects().sendMessage(operator, true, CollectionsUtil.wrapAll(bodyList.toArray(new String[] {}), (bodyItem) -> replaceListVars(center.formats().formatMessageWithPunishment(bodyItem, punishment), page, maxPage, nextPageCmd)));
		});
		center.subjects().sendMessage(operator, true, CollectionsUtil.wrapAll(footerList.toArray(new String[] {}), (footerItem) -> replaceListVars(footerItem, page, maxPage, nextPageCmd)));
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
		Set<Punishment> applicable = center.punishments().getActiveCopy();
		applicable.removeIf((punishment) -> !punishment.subject().equals(target));
		Punishment banPunishment = null;
		Punishment mutePunishment = null;
		int warns = 0;
		for (Punishment punishment : applicable) {
			if (PunishmentType.BAN.equals(punishment.type())) {
				banPunishment = punishment;
			} else if (PunishmentType.MUTE.equals(punishment.type())) {
				mutePunishment = punishment;
			} else if (PunishmentType.WARN.equals(punishment.type())) {
				warns++;
			}
		}
		final int warnCount = warns;
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
		String banStatus = banPunishment != null ? center.formats().formatMessageWithPunishment(other_status_layout.get("ban-status.banned"), banPunishment) : other_status_layout.get("ban-status.not-banned");
		String muteStatus = mutePunishment != null ? center.formats().formatMessageWithPunishment(other_status_layout.get("mute-status.muted"), mutePunishment) : other_status_layout.get("mute-status.not-muted");
		String warnStatus = warnCount != 0 ? other_status_layout.get("warn-status.warn-count") : other_status_layout.get("warn-status.no-warns");
		String targetDisplay = center.formats().formatSubject(target);
		String[] msgs = other_status_layout_body.toArray(new String[0]);
		center.subjects().sendMessage(operator, CollectionsUtil.wrapAll(msgs, (msg) -> msg.replace("%TARGET%", targetDisplay).replace("%HEADER%", header).replace("%INFO%", info).replace("%BAN_STATUS%", banStatus).replace("%MUTE_STATUS%", muteStatus).replace("%WARN_STATUS%", warnStatus).replace("%WARN_COUNT%", Integer.toString(warnCount)).replace("%CMD_WARNS%", "arimbans warns " + targetDisplay)));
	}
	
	private void ipsCmd(Subject operator, Subject target) {
		String[] msgs = other_ips_layout_body.toArray(new String[0]);
		String targetDisplay = center.formats().formatSubject(target);
		List<String> ips = null;
		try {
			ips = center.resolver().getIps(target.getUUID());
		} catch (MissingCacheException ex) {}
		if (ips == null || ips.isEmpty()) {
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
		String[] msgs = other_geoip_layout.toArray(new String[] {});
		String targetDisplay = center.formats().formatSubject(target);
		GeoIpInfo geoip;
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
				name = center.resolver().resolveUUID(uuid, center.environment().isOnlineMode());
			} catch (PlayerNotFoundException ex) {
				center.logs().logError(ex);
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
		if (!ROLLBACK_ALL_ARG.equals(numberArg)) {
			max = parseNumber(numberArg, 0);
			if (max == 0) {
				center.subjects().sendMessage(operator, other_rollback_error_invalidnumber.replace("%NUMBER%", numberArg));
				return;
			}
		} else {
			max = Integer.MAX_VALUE;
		}
		ArrayList<Punishment> applicable = new ArrayList<Punishment>(center.punishments().getActiveCopy());
		applicable.sort(DATE_LATEST_FIRST_COMPARATOR);
		int n = 0;
		for (Iterator<Punishment> it = applicable.iterator(); it.hasNext();) {
			Punishment punishment = it.next();
			if (punishment.operator().equals(target)) {
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
		boolean succeed = false;
		while (!succeed) {
			try {
				center.punishments().removePunishments(applicable.toArray(new Punishment[] {}));
				succeed = true;
			} catch (MissingPunishmentException ex) {
				applicable.remove(ex.getNonExistentPunishment());
				if (applicable.isEmpty()) {
					center.subjects().sendMessage(operator, other_rollback_error_notfound.replace("%TARGET%", center.formats().formatSubject(target)));
					return;
				}
			}
		}
		center.subjects().sendMessage(operator, other_rollback_successful_message.replace("%NUMBER%", numberArg).replace("%TARGET%", center.formats().formatSubject(target)));
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
		
		String base1 = "commands.confirm-unpunish.";
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
				break;
			case EDITREASON: // both cases fall-through to here
			case RELOAD:
				successful.put(category, center.config().getMessagesString(leadKey + "successful"));
				break;
			default:
				break;
			}
		}
		
		for (PunishmentType type : PunishmentType.values()) {
			String leadKey1 = center.config().keyString(type, CommandType.Category.ADD);
			String leadKey2 = center.config().keyString(type, CommandType.Category.REMOVE);
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
				noSilent.put(type, center.config().getMessagesString(leadKey1 + "permission.args.no-silent"));
				noPassive.put(type, center.config().getMessagesString(leadKey1 + "permission.args.no-passive"));
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
		other_alts_error_notfound = center.config().getMessagesString("other.alts.error.not-found");
		
		other_rollback_error_notfound = center.config().getMessagesString("other.rollback.error.not-found");
		other_rollback_error_invalidnumber = center.config().getMessagesString("other.rollback.error.invalid-number");
		other_rollback_successful_detail = center.config().getMessagesString("other.rollback.successful.detail");
		other_rollback_successful_message = center.config().getMessagesString("other.rollback.successful.message");
		
		other_editreason_error_notfound = center.config().getMessagesString("other.editreason.error.not-found");
		
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
