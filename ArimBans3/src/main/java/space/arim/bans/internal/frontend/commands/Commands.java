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
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.exception.InvalidCommandTypeException;

public class Commands implements CommandsMaster {
	
	private final ArimBans center;
	
	private String base_perm_msg;
	private String invalid_target;
	private static final String basePerm = "arimbans.commands";
	
	private final ConcurrentHashMap<PunishmentType, String> usagePun = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, String> permCmd = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, String> permIp = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, String> exempt = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, String> permTime = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, List<Integer>> durPerms = new ConcurrentHashMap<PunishmentType, List<Integer>>();
	
	// TODO Load these maps from messages.yml in #refreshMessages()
	private final ConcurrentHashMap<CommandType, String> listPermCmd = new ConcurrentHashMap<CommandType, String>();
	private final ConcurrentHashMap<CommandType, Integer> perPage = new ConcurrentHashMap<CommandType, Integer>();
	private final ConcurrentHashMap<CommandType, String> maxPage = new ConcurrentHashMap<CommandType, String>();
	private final ConcurrentHashMap<CommandType, String> noPage = new ConcurrentHashMap<CommandType, String>();
	
	public Commands(ArimBans center) {
		this.center = center;
		for (PunishmentType pun : PunishmentType.values()) {
			usagePun.put(pun, ArimBansLibrary.INVALID_STRING_CODE);
			permCmd.put(pun, ArimBansLibrary.INVALID_STRING_CODE);
			permIp.put(pun, ArimBansLibrary.INVALID_STRING_CODE);
			exempt.put(pun, ArimBansLibrary.INVALID_STRING_CODE);
			permTime.put(pun, ArimBansLibrary.INVALID_STRING_CODE);
		}
		for (CommandType cmd : CommandType.values()) {
			listPermCmd.put(cmd, ArimBansLibrary.INVALID_STRING_CODE);
			perPage.put(cmd, 0);
			maxPage.put(cmd, ArimBansLibrary.INVALID_STRING_CODE);
			noPage.put(cmd, ArimBansLibrary.INVALID_STRING_CODE);
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
		if (!center.environment().hasPermission(subject, basePerm)) {
			noPermission(subject);
			return false;
		}
		return true;
	}
	
	private boolean checkPermission(Subject subject, CommandType command) {
		if (!checkPermBase(subject)) {
			return false;
		}
		if (!center.environment().hasPermission(subject, command.permission())) {
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
			if (rawArgs.length > 1) {
				execute(subject, command, chopOffOne(rawArgs));
			} else {
				usage(subject, command);
			}
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
		if (command.isGeneralListing()) {
			listGeneralCmd(operator, command, args[0]);
		} else if (command.isListing() || command.isAddition() || command.isRemoval()) {
			if (args.length <= 0) {
				usage(operator, command);
				return;
			}
			Subject target;
			try {
				target = center.subjects().parseSubject(args[0], false);
			} catch (IllegalArgumentException ex) {
				center.environment().sendMessage(operator, invalid_target.replaceAll("%TARGET%", args[0]));
				return;
			}
			if (!center.environment().isOnline(target)) {
				center.environment().sendMessage(operator, invalid_target.replaceAll("%TARGET%", center.formats().formatSubject(target)));
				return;
			}
			if (command.isAddition()) {
				punCmd(operator, target, command, chopOffOne(args));
			} else if (command.isRemoval()) {
				unpunCmd(operator, target, command);
			} else if (command.isListing()) {
				listCmd(operator, target, command, args[1]);
			}
		} else {
			throw new InvalidCommandTypeException(command);
		}
	}
	
	private void punCmd(Subject operator, Subject target, CommandType command, String[] args) {
		
		
	}
	
	private void unpunCmd(Subject operator, Subject target, CommandType command) {
		
	}
	
	private void listGeneralCmd(Subject operator, CommandType command, String pageInput) {
		int page = parsePage(pageInput);
	}
	
	private void listCmd(Subject operator, Subject target, CommandType command, String pageInput) {
		int page = parsePage(pageInput);
		list(operator, new Lister<Punishment>(page, perPage.get(command), maxPage.get(command), noPage.get(command)) {
			@Override
			Set<Punishment> getAll() {
				return null;
			}
			@Override
			boolean check(Punishment object) {
				return false;
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
	
	private void list(Subject operator, Lister<Punishment> lister) {
		ArrayList<Punishment> punishments = new ArrayList<Punishment>(lister.getAll());
		for (Iterator<Punishment> it = punishments.iterator(); it.hasNext();) {
			if (!lister.check(it.next())) {
				it.remove();
			}
		}
		if (punishments.isEmpty()) {
			center.environment().sendMessage(operator, lister.noPagesMsg);
			return;
		}
		int maxPage = (punishments.size() - 1)/lister.perPage + 1;
		if (lister.page > maxPage) {
			center.environment().sendMessage(operator, lister.maxPagesMsg);
			return;
		}
		for (String h : lister.getHeader()) {
			center.environment().sendMessage(operator, h.replaceAll("%PAGE%", Integer.toString(lister.page)).replaceAll("%MAXPAGE%", Integer.toString(maxPage)));
		}
		punishments.sort(new Comparator<Punishment>() {
			@Override
			public int compare(Punishment p1, Punishment p2) {
				return (int) (p1.date() - p2.date());
			}
		});
		for (Punishment p : punishments) {
			for (String s : lister.getFormat()) {
				center.environment().sendMessage(operator, encodeVars(s, p));
			}
		}
		for (String f : lister.getFooter()) {
			center.environment().sendMessage(operator, f.replaceAll("%PAGE%", Integer.toString(lister.page)).replaceAll("%MAXPAGE%", Integer.toString(maxPage)));
		}
	}

	@Override
	public void usage(Subject subject) {
		if (!checkPermBase(subject)) {
			return;
		}
	}
	
	@Override
	public void usage(Subject subject, CommandType command) {
		if (!checkPermission(subject, command)) {
			return;
		}
	}

	@Override
	public void refreshConfig() {
		
	}
	
	@Override
	public void refreshMessages() {
		
		base_perm_msg = center.config().getMessagesString("all.base-permission-message");
		invalid_target = center.config().getMessagesString("all.invalid-target");
		
		String usageBan = center.config().getMessagesString("bans.usage");
		String usageMute = center.config().getMessagesString("mutes.usage");
		String usageWarn = center.config().getMessagesString("warns.usage");
		String usageKick = center.config().getMessagesString("kicks.usage");
		
		String noPermBan = center.config().getMessagesString("bans.permission.command");
		String noPermMute = center.config().getMessagesString("mutes.permission.command");
		String noPermWarn = center.config().getMessagesString("warns.permission.command");
		String noPermKick = center.config().getMessagesString("kicks.permission.command");
		
		String noPermIpBan = center.config().getMessagesString("bans.permission.no-ip");
		String noPermIpMute = center.config().getMessagesString("mutes.permission.no-ip");
		String noPermIpWarn = center.config().getMessagesString("warns.permission.no-ip");
		String noPermIpKick = center.config().getMessagesString("kicks.permission.no-ip");
		
		String exemptBan = center.config().getMessagesString("bans.permission.exempt");
		String exemptMute = center.config().getMessagesString("mutes.permission.exempt");
		String exemptWarn = center.config().getMessagesString("warns.permission.exempt");
		String exemptKick = center.config().getMessagesString("kicks.permission.exempt");
		
		String noPermTimeBan = center.config().getMessagesString("bans.permission.time");
		String noPermTimeMute = center.config().getMessagesString("mutes.permission.time");
		String noPermTimeWarn = center.config().getMessagesString("kicks.permission.time");
		
		List<Integer> durPermsBan = center.config().getMessagesInts(".permission.dur-perms");
		List<Integer> durPermsMute = center.config().getMessagesInts(".permission.dur-perms");
		List<Integer> durPermsWarn = center.config().getMessagesInts(".permission.dur-perms");
		durPermsBan.add(-1);
		durPermsMute.add(-1);
		durPermsWarn.add(-1);
		
		usagePun.put(PunishmentType.BAN, usageBan);
		usagePun.put(PunishmentType.MUTE, usageMute);
		usagePun.put(PunishmentType.WARN, usageWarn);
		usagePun.put(PunishmentType.KICK, usageKick);
		
		permCmd.put(PunishmentType.BAN, noPermBan);
		permCmd.put(PunishmentType.MUTE, noPermMute);
		permCmd.put(PunishmentType.WARN, noPermWarn);
		permCmd.put(PunishmentType.KICK, noPermKick);
		
		permIp.put(PunishmentType.BAN, noPermIpBan);
		permIp.put(PunishmentType.MUTE, noPermIpMute);
		permIp.put(PunishmentType.WARN, noPermIpWarn);
		permIp.put(PunishmentType.KICK, noPermIpKick);
		
		exempt.put(PunishmentType.BAN,  exemptBan);
		exempt.put(PunishmentType.MUTE, exemptMute);
		exempt.put(PunishmentType.WARN, exemptWarn);
		exempt.put(PunishmentType.KICK, exemptKick);
		
		permTime.put(PunishmentType.BAN, noPermTimeBan);
		permTime.put(PunishmentType.MUTE, noPermTimeMute);
		permTime.put(PunishmentType.WARN, noPermTimeWarn);
		
		durPerms.put(PunishmentType.BAN, durPermsBan);
		durPerms.put(PunishmentType.MUTE, durPermsMute);
		durPerms.put(PunishmentType.WARN, durPermsWarn);
		durPerms.put(PunishmentType.KICK, Arrays.asList(-1));
		
	}

	@Override
	public void noPermission(Subject subject) {
		center.environment().sendMessage(subject, base_perm_msg);
	}

	@Override
	public void noPermission(Subject subject, CommandType command) {
		if (command.isAddition() || command.isRemoval()) {
			
		}
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
