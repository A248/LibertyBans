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
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.exception.InternalStateException;

public class Commands implements CommandsMaster {
	
	private final ArimBans center;
	
	private String base_perm_msg;
	private String invalid_target;
	private String base_usage;
	private static final String basePerm = "arimbans.commands";
	
	private final ConcurrentHashMap<CommandType, String> usage = new ConcurrentHashMap<CommandType, String>();
	private final ConcurrentHashMap<CommandType, String> perm = new ConcurrentHashMap<CommandType, String>();
	
	// Maps related to punishment additions.
	private final ConcurrentHashMap<PunishmentType, String> permTime = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, List<String>> durPerms = new ConcurrentHashMap<PunishmentType, List<String>>();
	private final ConcurrentHashMap<PunishmentType, String> exempt = new ConcurrentHashMap<PunishmentType, String>();
	String additions_bans_error_conflicting = "&c&o%TARGET%&r&7is already banned!";
	String additions_mutes_error_conflicting = "&c&o%TARGET%&r&7is already muted!";
	
	// TODO Load these maps from messages.yml in #refreshMessages()
	private final ConcurrentHashMap<CommandType, Integer> perPage = new ConcurrentHashMap<CommandType, Integer>();
	private final ConcurrentHashMap<CommandType, String> maxPage = new ConcurrentHashMap<CommandType, String>();
	private final ConcurrentHashMap<CommandType, String> noPage = new ConcurrentHashMap<CommandType, String>();
	private final ConcurrentHashMap<CommandType, List<String>> header = new ConcurrentHashMap<CommandType, List<String>>();
	private final ConcurrentHashMap<CommandType, List<String>> body = new ConcurrentHashMap<CommandType, List<String>>();
	private final ConcurrentHashMap<CommandType, List<String>> footer = new ConcurrentHashMap<CommandType, List<String>>();
	
	public Commands(ArimBans center) {
		this.center = center;
		String invalid_string = ArimBansLibrary.INVALID_STRING_CODE;
		for (PunishmentType pun : PunishmentType.values()) {
			permTime.put(pun, invalid_string);
			durPerms.put(pun, Arrays.asList(invalid_string));
			exempt.put(pun, invalid_string);
		}
		for (CommandType cmd : CommandType.values()) {
			usage.put(cmd, invalid_string);
			perm.put(cmd, invalid_string);
			perPage.put(cmd, 0);
			maxPage.put(cmd, invalid_string);
			noPage.put(cmd, invalid_string);
			header.put(cmd, Arrays.asList(invalid_string));
			body.put(cmd, Arrays.asList(invalid_string));
			footer.put(cmd, Arrays.asList(invalid_string));
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
	
	private boolean checkPermission(Subject subject, CommandType command) {
		if (!checkPermBase(subject)) {
			return false;
		}
		if (!center.subjects().hasPermission(subject, command.getPermission())) {
			noPermission(subject, command);
			return false;
		}
		return true;
	}
	
	private String encodeVars(String message, Punishment p) {
		return message.replaceAll("%SUBJECT%", center.formats().formatSubject(p.subject())).replaceAll("%OPERATOR%", center.formats().formatSubject(p.operator())).replaceAll("%REASON%", p.reason()).replaceAll("%EXP_REL%", center.formats().formatTime(p.expiration(), false)).replaceAll("%EXP_ABS%", center.formats().formatTime(p.expiration(), true)).replaceAll("%DATE_REL%", center.formats().formatTime(p.date(), false)).replaceAll("%DATE_ABS%", center.formats().formatTime(p.date(), true));
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
			Subject target;
			try {
				target = center.subjects().parseSubject(args[0], false);
			} catch (IllegalArgumentException ex) {
				center.subjects().sendMessage(operator, invalid_target.replaceAll("%TARGET%", args[0]));
				return;
			}
			if (!center.environment().isOnline(target)) {
				center.subjects().sendMessage(operator, invalid_target.replaceAll("%TARGET%", center.formats().formatSubject(target)));
				return;
			}
			if (command.category().equals(Category.ADD)) {
				punCmd(operator, target, command, chopOffOne(args));
			} else if (command.category().equals(Category.REMOVE)) {
				unpunCmd(operator, target, command);
			} else if (command.category().equals(Category.LIST)) {
				listCmd(operator, target, command, args[1]);
			} else if (command.category().equals(Category.OTHER)) {
				otherCmd(operator, target, command, chopOffOne(args));
			}
		}
	}
	
	private void punCmd(Subject operator, Subject target, CommandType command, String[] args) {
		
		
	}
	
	private void unpunCmd(Subject operator, Subject target, CommandType command) {
		
	}
	
	private void listCmd(Subject operator, Subject target, CommandType command, String pageInput) {
		int page = parsePage(pageInput);
		list(operator, new Lister<Punishment>(page, perPage.get(command), maxPage.get(command), noPage.get(command)) {
			@Override
			Set<Punishment> getAll() {
				return center.getHistory(target);
			}
			@Override
			boolean check(Punishment object) {
				return true;
			}
			@Override
			List<String> getHeader() {
				return null;
			}
			@Override
			List<String> getFormat() {
				return null;
			}
			@Override
			List<String> getFooter() {
				return null;
			}
		});
	}
	
	private void noTargetCmd(Subject operator, CommandType command, String[] args) {
		
	}
	
	private void otherCmd(Subject operator, Subject target, CommandType command, String[] args) {
		
	}
	
	private void list(Subject operator, Lister<Punishment> lister) {
		ArrayList<Punishment> punishments = new ArrayList<Punishment>(lister.getAll());
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
			center.subjects().sendMessage(operator, lister.maxPagesMsg);
			return;
		}
		List<String> header = lister.getHeader();
		List<String> body = lister.getFormat();
		List<String> footer = lister.getFooter();
		for (int n = 0; n < header.size(); n++) {
			header.set(n, header.get(n).replaceAll("%PAGE%", Integer.toString(lister.page)).replaceAll("%MAXPAGE%", Integer.toString(maxPage)));
		}
		for (int n = 0; n < footer.size(); n++) {
			footer.set(n, footer.get(n).replaceAll("%PAGE%", Integer.toString(lister.page)).replaceAll("%MAXPAGE%", Integer.toString(maxPage)));
		}
		punishments.sort(new Comparator<Punishment>() {
			@Override
			public int compare(Punishment p1, Punishment p2) {
				return (int) (p1.date() - p2.date());
			}
		});
		center.subjects().sendMessage(operator, header.toArray(new String[0]));
		for (Punishment p : punishments) {
			String[] msgs = body.toArray(new String[0]);
			for (int n = 0; n < msgs.length; n++) {
				msgs[n] = encodeVars(msgs[n], p);
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
	
	@SuppressWarnings("incomplete-switch")
	@Override
	public void refreshMessages() {
		
		base_perm_msg = center.config().getMessagesString("all.base-permission-message");
		invalid_target = center.config().getMessagesString("all.invalid-target");
		base_usage = center.config().getMessagesString("all.usage");
		
		String additions_bans_usage = center.config().getMessagesString("additions.bans.usage");
		String additions_mutes_usage = center.config().getMessagesString("additions.mutes.usage");
		String additions_warns_usage = center.config().getMessagesString("additions.warns.usage");
		String additions_kicks_usage = center.config().getMessagesString("additions.kicks.usage");
		
		String additions_bans_permission_command = center.config().getMessagesString("additions.bans.permission.command");
		String additions_mutes_permission_command = center.config().getMessagesString("additions.mutes.permission.command");
		String additions_warns_permission_command = center.config().getMessagesString("additions.warns.permission.command");
		String additions_kicks_permission_command = center.config().getMessagesString("additions.kicks.permission.command");
		
		String additions_bans_permission_noip = center.config().getMessagesString("additions.bans.permission.no-ip");
		String additions_mutes_permission_noip = center.config().getMessagesString("additions.mutes.permission.no-ip");
		String additions_warns_permission_noip = center.config().getMessagesString("additions.warns.permission.no-ip");
		String additions_kicks_permission_noip = center.config().getMessagesString("additions.kicks.permission.no-ip");
		
		String additions_bans_permission_time = center.config().getMessagesString("additions.bans.permission.time");
		String additions_mutes_permission_time = center.config().getMessagesString("additions.mutes.permission.time");
		String additions_warns_permission_time = center.config().getMessagesString("additions.warns.permission.time");
		
		List<String> additions_bans_permission_durPerms = center.config().getMessagesStrings("additions.bans.permission.dur-perms");
		List<String> additions_mutes_permission_durPerms = center.config().getMessagesStrings("additions.bans.permission.dur-perms");
		List<String> additions_warns_permission_durPerms = center.config().getMessagesStrings("additions.mutes.permission.dur-perms");
		
		additions_bans_error_conflicting = center.config().getMessagesString("additions.bans.error.conflicting");
		additions_mutes_error_conflicting = center.config().getMessagesString("additions.mutes.error.conflicting");
		
		String additions_bans_error_exempt = center.config().getMessagesString("additions.bans.error.exempt");
		String additions_mutes_error_exempt = center.config().getMessagesString("additions.mutes.error.exempt");
		String additions_warns_error_exempt = center.config().getMessagesString("additions.warns.error.exempt");
		String additions_kicks_error_exempt = center.config().getMessagesString("additions.kicks.error.exempt");
		
		/*
		String removals_unbans_usage = center.config().getMessagesString("removals.unbans.usage");
		String removals_unmutes_usage = center.config().getMessagesString("removals.unmutes.usage");
		String removals_unwarns_usage = center.config().getMessagesString("removals.unwarns.usage");
		
		String banlist_usage = center.config().getMessagesString("banlist.usage");
		String mutelist_usage = center.config().getMessagesString("banlist.usage");
		String history_usage = center.config().getMessagesString("history.usage");
		String warns_usage = center.config().getMessagesString("warns.usage");
		
		String check_usage = center.config().getMessagesString("check.usage");
		
		String noPermBan = center.config().getMessagesString("bans.permission.command");
		String noPermMute = center.config().getMessagesString("mutes.permission.command");
		String noPermWarn = center.config().getMessagesString("warns.permission.command");
		String noPermKick = center.config().getMessagesString("kicks.permission.command");
		
		String noPermIpBan = center.config().getMessagesString("bans.permission.no-ip");
		String noPermIpMute = center.config().getMessagesString("mutes.permission.no-ip");
		String noPermIpWarn = center.config().getMessagesString("warns.permission.no-ip");
		String noPermIpKick = center.config().getMessagesString("kicks.permission.no-ip");
		

		*/
		
		for (CommandType cmd : CommandType.values()) {
			switch (cmd.subCategory()) {
			case BAN:
				usage.put(cmd, additions_bans_usage);
				perm.put(cmd, (cmd.ipSpec().equals(IpSpec.IP)) ? additions_bans_permission_noip : additions_bans_permission_command);
				break;
			case MUTE:
				usage.put(cmd, additions_mutes_usage);
				perm.put(cmd, (cmd.ipSpec().equals(IpSpec.IP)) ? additions_mutes_permission_noip : additions_mutes_permission_command);
				break;
			case WARN:
				usage.put(cmd, additions_warns_usage);
				perm.put(cmd, (cmd.ipSpec().equals(IpSpec.IP)) ? additions_warns_permission_noip : additions_warns_permission_command);
				break;
			case KICK:
				usage.put(cmd, additions_kicks_usage);
				perm.put(cmd, (cmd.ipSpec().equals(IpSpec.IP)) ? additions_kicks_permission_noip : additions_kicks_permission_command);
				break;
			default:
				throw new InternalStateException("WIP!");
			}
		}
		
		permTime.put(PunishmentType.BAN, additions_bans_permission_time);
		permTime.put(PunishmentType.MUTE, additions_mutes_permission_time);
		permTime.put(PunishmentType.WARN, additions_warns_permission_time);
		
		durPerms.put(PunishmentType.BAN, additions_bans_permission_durPerms);
		durPerms.put(PunishmentType.MUTE, additions_mutes_permission_durPerms);
		durPerms.put(PunishmentType.WARN, additions_warns_permission_durPerms);
		
		exempt.put(PunishmentType.BAN,  additions_bans_error_exempt);
		exempt.put(PunishmentType.MUTE, additions_mutes_error_exempt);
		exempt.put(PunishmentType.WARN, additions_warns_error_exempt);
		exempt.put(PunishmentType.KICK, additions_kicks_error_exempt);
		
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
	
	Lister(int page, int perPage, String maxPagesMsg, String noPagesMsg) {
		this.page = page;
		this.perPage = perPage;
		this.maxPagesMsg = maxPagesMsg;
		this.noPagesMsg = noPagesMsg;
	}
	
	abstract Set<T> getAll();
	
	abstract boolean check(T object);
	
	abstract List<String> getHeader();
	
	abstract List<String> getFormat();
	
	abstract List<String> getFooter();
	
}
