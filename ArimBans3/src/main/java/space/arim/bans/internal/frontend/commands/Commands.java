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
import space.arim.bans.api.exception.PlayerNotFoundException;

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
	String additions_bans_error_conflicting;
	String additions_mutes_error_conflicting;
	private final ConcurrentHashMap<SubCategory, String> successful = new ConcurrentHashMap<SubCategory, String>();
	private final ConcurrentHashMap<SubCategory, String> notification = new ConcurrentHashMap<SubCategory, String>();
	private final ConcurrentHashMap<PunishmentType, List<String>> layout = new ConcurrentHashMap<PunishmentType, List<String>>();
	
	private final ConcurrentHashMap<PunishmentType, String> notfound = new ConcurrentHashMap<PunishmentType, String>();
	String removals_warns_error_confirm;
	
	private final ConcurrentHashMap<SubCategory, Integer> perPage = new ConcurrentHashMap<SubCategory, Integer>();
	private final ConcurrentHashMap<SubCategory, String> maxPage = new ConcurrentHashMap<SubCategory, String>();
	private final ConcurrentHashMap<SubCategory, String> noPage = new ConcurrentHashMap<SubCategory, String>();
	private final ConcurrentHashMap<SubCategory, List<String>> header = new ConcurrentHashMap<SubCategory, List<String>>();
	private final ConcurrentHashMap<SubCategory, List<String>> body = new ConcurrentHashMap<SubCategory, List<String>>();
	private final ConcurrentHashMap<SubCategory, List<String>> footer = new ConcurrentHashMap<SubCategory, List<String>>();
	
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
		removals_warns_error_confirm = invalid_string;
		for (PunishmentType pun : PunishmentType.values()) {
			notfound.put(pun, invalid_string);
			permTime.put(pun, invalid_string);
			durations.put(pun, invalid_strings);
			exempt.put(pun, invalid_string);
			layout.put(pun, invalid_strings);
		}
		for (SubCategory cat : SubCategory.values()) {
			successful.put(cat, invalid_string);
			notification.put(cat, invalid_string);
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

	private String[] chopOffOne(String[] input) {
		String[] output = new String[input.length - 2];
		for (int n = 0; n < output.length; n++) {
			output[n] = input[n + 1];
		}
		return output;
	}
	
	private int parsePage(String input) {
		try {
			int page = 0;
			page = Integer.parseInt(input);
			return (page <= 0) ? 1 : page;
		} catch (NumberFormatException ex) {
			return 1;
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
	
	private String keyString(PunishmentType type) {
		switch (type) {
		case BAN:
			return "bans.";
		case MUTE:
			return "mutes.";
		case WARN:
			return "warns.";
		case KICK:
			return "kicks.";
		default:
			throw new InternalStateException("What other punishment type is there?!?");
		}
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
			execute(subject, command, (rawArgs.length > 1) ? chopOffOne(rawArgs) : new String[] {});
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
			noTargetCmd(operator, command, args);
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
			if (command.category().equals(Category.ADD)) {
				punCmd(operator, target, command, chopOffOne(args));
			} else if (command.category().equals(Category.REMOVE)) {
				unpunCmd(operator, target, command, chopOffOne(args));
			} else if (command.category().equals(Category.LIST)) {
				listCmd(operator, target, command, args[1]);
			} else if (command.category().equals(Category.OTHER)) {
				otherCmd(operator, target, command, chopOffOne(args));
			}
		}
	}
	
	private String notifyPerm(PunishmentType type) {
		return "arimbans." + type.name() + ".notify";
	}
	
	// TODO check dur perms, detect time argument, concatenate reason argument
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
				args = chopOffOne(args);
			}
			if (span == -1L && max != -1L || span > max) {
				center.subjects().sendMessage(operator, permTime.get(type));
				return;
			}
		} else {
			span = -1L;
		}
		String reason;
		if (args.length == 0) {
			if (permit_blank_reason) {
				reason = default_reason;
			} else {
				usage(operator, command);
				return;
			}
		} else {
			reason = concat(args);
		}
		Punishment punishment = new Punishment(type, target, operator, reason, (span == -1L) ? span : span + System.currentTimeMillis());
		try {
			center.punishments().addPunishments(punishment);
			center.subjects().sendMessage(operator, center.formats().formatMessageWithPunishment(successful.get(command.subCategory()), punishment));
			center.subjects().sendForPermission(notifyPerm(type), center.formats().formatMessageWithPunishment(notification.get(command.subCategory()), punishment));
		} catch (ConflictingPunishmentException ex) {
			String conflict = (punishment.type().equals(PunishmentType.BAN)) ? additions_bans_error_conflicting : additions_mutes_error_conflicting;
			center.subjects().sendMessage(operator, conflict.replace("%TARGET%", center.formats().formatSubject(punishment.subject())));
		}
	}
	
	private void unpunCmd(Subject operator, Subject target, CommandType command, String[] args) {
		PunishmentType type = applicableType(command);
		if (command.subCategory().equals(SubCategory.UNWARN)) {
			if (args.length > 0) {
				if (args[0].equals("internal_bydate")) {
					try {
						long date = Long.parseLong(args[1]);
						Set<Punishment> active = center.punishments().getAllPunishments();
						for (Punishment punishment : active) {
							if (punishment.date() == date) {
								center.punishments().removePunishments(punishment);
								center.subjects().sendMessage(operator, center.formats().formatMessageWithPunishment(successful.get(command.subCategory()), punishment));
								center.subjects().sendForPermission(notifyPerm(PunishmentType.WARN), center.formats().formatMessageWithPunishment(notification.get(command.subCategory()), punishment));
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
						String cmd = getCmdBaseString(command) + " internal_bydate " + punishment.date();
						center.subjects().sendMessage(operator, center.formats().formatMessageWithPunishment(removals_warns_error_confirm, punishment).replace("%CMD%", cmd));
						return;
					} catch (NumberFormatException ex) {}
				}
			}
			usage(operator, command);
			return;
		}
		try {
			Punishment punishment = center.punishments().getPunishment(target, type);
			center.punishments().removePunishments(punishment);
			center.subjects().sendMessage(operator, center.formats().formatMessageWithPunishment(successful.get(command.subCategory()), punishment));
			center.subjects().sendForPermission(notifyPerm(type), center.formats().formatMessageWithPunishment(notification.get(command.subCategory()), punishment));
		} catch (MissingPunishmentException ex) {
			center.subjects().sendMessage(operator, notfound.get(type).replace("%TARGET%", center.formats().formatSubject(target)));
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
		default:
			return center.punishments().getPunishments(target);
		}
	}
	
	private String getCmdBaseString(CommandType command) {
		return "arimbans " + command.name();
	}
	
	private void listCmd(Subject operator, Subject target, CommandType command, String pageInput) {
		Set<Punishment> applicable = getAllForListParams(target, command);
		list(operator, new Lister<Punishment>(parsePage(pageInput), perPage.get(command.subCategory()), maxPage.get(command.subCategory()), noPage.get(command.subCategory()), applicable, header.get(command.subCategory()), body.get(command.subCategory()), footer.get(command.subCategory())) {
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
	
	private void noTargetCmd(Subject operator, CommandType command, String[] args) {
		
	}
	
	private void otherCmd(Subject operator, Subject target, CommandType command, String[] args) {
		
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
				msgs[n] = center.formats().formatMessageWithPunishment(msgs[n], p);
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
	public void refreshConfig() {
		permit_blank_reason = center.config().getConfigBoolean("commands.reasons.permit-blank");
		default_reason = center.config().getConfigString("commands.reasons.default-reason");
	}
	
	@Override
	public void refreshMessages() {
		
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
			String keyString = keyString(type);
			String leadKey1 = "additions." + keyString;
			String leadKey2 = "removals." + keyString;
			SubCategory categoryAdd = fromPunishmentType(type, true);
			SubCategory categoryRemove = fromPunishmentType(type, false);
			switch (type) {
			case BAN: // falls through to warn case
			case MUTE: // falls through to warn case
			case WARN:
				// removals do not apply to kicks
				notfound.put(type, center.config().getMessagesString(leadKey2 + "error.not-found"));
				successful.put(categoryRemove, center.config().getMessagesString(leadKey2 + "successful.message"));
				notification.put(categoryRemove, center.config().getMessagesString(leadKey2 + "successful.notification"));
				// durations do not apply to kicks
				permTime.put(type, center.config().getMessagesString(leadKey1 + "permission.time"));
				durations.put(type, center.config().getMessagesStrings(leadKey1 + "permission.dur-perms"));
				// then falls through to kick case
			case KICK:
				exempt.put(type, center.config().getMessagesString(leadKey1 + "error.exempt"));
				successful.put(categoryAdd, center.config().getMessagesString(leadKey1 + "successful.message"));
				notification.put(categoryAdd, center.config().getMessagesString(leadKey1 + "successful.notification"));
				layout.put(type, center.config().getMessagesStrings(leadKey1 + "layout"));
				break;
			default:
				throw new InternalStateException("What other punishment type is there?!?");
			}
		}
		
	}
	
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
