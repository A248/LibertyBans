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

import space.arim.bans.ArimBans;
import space.arim.bans.api.CommandType;
import space.arim.bans.api.Subject;

// TODO Make this class work
public class Commands implements CommandsMaster {
	
	private final ArimBans center;
	
	private String perm_display;

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
			CommandType type = parseCommand(rawArgs[0]);
			if (rawArgs.length > 1) {
				execute(subject, type, chopOffOne(rawArgs));
			} else {
				usage(subject, type);
			}
		} catch (IllegalArgumentException ex) {
			usage(subject);
		}
	}
	
	@Override
	public void execute(Subject subject, CommandType command, String[] extraArgs) {
		if (extraArgs.length > 0) {
			exec(subject, command, extraArgs);
		}
		usage(subject);
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
		perm_display = center.config().getString("formatting.permanent-display");
	}

	@Override
	public void noPermission(Subject subject) {
		
	}

	@Override
	public void noPermission(Subject subject, CommandType command) {
		
	}
}
