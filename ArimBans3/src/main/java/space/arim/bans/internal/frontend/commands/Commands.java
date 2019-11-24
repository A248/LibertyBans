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

import java.util.concurrent.ConcurrentHashMap;

import space.arim.bans.ArimBans;
import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.CommandType;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;

// TODO Make this class work
public class Commands implements CommandsMaster {
	
	private final ArimBans center;
	
	private String perm_display;
	private final ConcurrentHashMap<PunishmentType, String> permCmd = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, String> permIp = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, String> exempt = new ConcurrentHashMap<PunishmentType, String>();
	private final ConcurrentHashMap<PunishmentType, String> permTime = new ConcurrentHashMap<PunishmentType, String>();

	public Commands(ArimBans center) {
		this.center = center;
		refreshConfig();
	}

	public String formatTime(long unix) {
		if (unix < 0) {
			return perm_display;
		}
		return center.formats().fromUnix(unix);
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
		if (args.length > 0) {
			exec(subject, command, args);
		}
		usage(subject, command);
	}
	
	private void exec(Subject subject, CommandType command, String[] args) {
		
	}

	@Override
	public void usage(Subject subject, CommandType command) {
		
	}

	@Override
	public void usage(Subject subject) {

	}
	
	@Override
	public void close() {
		
	}

	@Override
	public void refreshConfig() {
		
		perm_display = center.config().getConfigString("formatting.permanent-display");
		
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
		
	}

	@Override
	public void noPermission(Subject subject) {
		
	}

	@Override
	public void noPermission(Subject subject, CommandType command) {
		
	}
	
}
