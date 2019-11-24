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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import space.arim.bans.ArimBans;
import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.CommandType;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.exception.InvalidCommandTypeException;

// TODO Make this class work
public class Commands implements CommandsMaster {
	
	private final ArimBans center;
	
	private String invalid_target;
	private String no_target_console;
	
	private final ConcurrentHashMap<PunishmentType, String> usagePun = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, String> permCmd = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, String> permIp = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, String> exempt = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, String> permTime = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, List<Integer>> durPerms = new ConcurrentHashMap<PunishmentType, List<Integer>>();

	public Commands(ArimBans center) {
		this.center = center;
		refreshConfig();
	}

	private String[] chopOffOne(String[] input) {
		String[] output = new String[input.length - 2];
		for (int n = 0; n < output.length; n++) {
			output[n] = input[n + 1];
		}
		return output;
	}
	
	@Override
	public void execute(Subject subject, String[] rawArgs) {
		try {
			CommandType command = parseCommand(rawArgs[0]);
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
		if (command.isGeneralListing()) {
			listGeneralCmd(operator, command, args);
		} else if (command.isListing() || command.isAddition() || command.isRemoval()) {
			if (args.length > 0) {
				Subject target;
				try {
					target = center.subjects().parseSubject(args[0]);
				} catch (IllegalArgumentException ex) {
					center.environment().sendMessage(operator, invalid_target.replaceAll("%TARGET%", args[0]));
					return;
				}
				if (!center.environment().isOnline(target)) {
					center.environment().sendMessage(operator, invalid_target.replaceAll("%TARGET%", center.formats().formatSubject(target)));
					return;
				}
			} else {
				usage(operator, command);
			}
		}
		throw new InvalidCommandTypeException(command);
	}
	
	private void punCmd(Subject operator, Subject target, CommandType command, String[] args) {
		
		
	}
	
	private void unpunCmd(Subject operator, Subject target, CommandType command, String[] args) {
		
	}
	
	private void listGeneralCmd(Subject operator, CommandType command, String[] args) {
		
	}
	
	private void listCmd(Subject operator, Subject target, CommandType command, String[] args) {
		
	}

	@Override
	public void usage(Subject subject, CommandType command) {
		
	}

	@Override
	public void usage(Subject subject) {

	}

	@Override
	public void refreshConfig() {
		
	}
	
	@Override
	public void refreshMessages() {
		
		invalid_target = center.config().getMessagesString("all.invalid-target");
		no_target_console = center.config().getMessagesString("all.no-target-console");
		
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
		permTime.put(PunishmentType.KICK, ArimBansLibrary.INVALID_STRING_CODE);
		
		durPerms.put(PunishmentType.BAN, durPermsBan);
		durPerms.put(PunishmentType.MUTE, durPermsMute);
		durPerms.put(PunishmentType.WARN, durPermsWarn);
		durPerms.put(PunishmentType.KICK, Arrays.asList(-1));
		
	}

	@Override
	public void noPermission(Subject subject) {
		
	}

	@Override
	public void noPermission(Subject subject, CommandType command) {
		
	}
	
}
