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
package space.arim.bans.internal.frontend.format;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.Subject;
import space.arim.bans.api.exception.InvalidSubjectException;
import space.arim.bans.api.exception.PlayerNotFoundException;

//TODO Make this class work
public class Formats implements FormatsMaster {
	
	private final ArimBans center;
	
	private SimpleDateFormat dateFormatter;
	
	private List<String> permanent_arguments = Arrays.asList("perm");
	
	private String permanent_display;
	private String console_display;

	public Formats(ArimBans center) {
		this.center = center;
	}

	@Override
	public String formatPunishment(Punishment punishment) {
		
		return center.config().getConfigString("messages");
	}
	
	@Override
	public String formatSubject(Subject subj) {
		switch (subj.getType()) {
		case PLAYER:
			try {
				return center.resolver().resolveUUID(subj.getUUID());
			} catch (PlayerNotFoundException ex) {
				throw new InvalidSubjectException("Subject's UUID could not be resolved to a name!", ex);
			}
		case IP:
			return subj.getIP();
		case CONSOLE:
			return console_display;
		default:
			throw new InvalidSubjectException(subj);
		}
	}

	@Override
	public String formatTime(long millis, boolean absolute) {
		if (millis < 0) {
			return permanent_display;
		}
		if (absolute) {
			return dateFormatter.format(new Date(millis));
		}
		return null;
	}
	
	@Override
	public long toMillis(String timespan) {
		long mult = 1_000_000;
		if (permanent_arguments.contains(timespan.toLowerCase()) || timespan.toLowerCase().equals("internal_perm")) {
			return -1L;
		} else if (timespan.contains("mo")) {
			mult = 2592000_000_000L;
			timespan = timespan.replace("mo", "");
		} else if (timespan.contains("d")) {
			mult = 86400_000_000L;
			timespan = timespan.replace("d", "");
		} else if (timespan.contains("h") || timespan.contains("hr")) {
			mult = 3600_000_000L;
			timespan = timespan.replace("hr", "").replace("h", "");
		} else if (timespan.contains("m")) {
			mult = 60_000_000L;
			timespan = timespan.replace("m", "");
		}
		try {
			return mult*Long.parseLong(timespan);
		} catch (NumberFormatException ex) {
			return 0;
		}
	}
	
	@Override
	public String getConsoleDisplay() {
		return console_display;
	}
	
	@Override
	public void refreshConfig() {
		
		try {
			dateFormatter = new SimpleDateFormat(center.config().getConfigString("formatting.date"));
		} catch (IllegalArgumentException ex) {
			dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		}
		
		dateFormatter.getCalendar().setTimeZone(TimeZone.getDefault());
		
		permanent_arguments = center.config().getConfigStrings("formatting.permanent-arguments");
		
		permanent_display = center.config().getConfigString("formatting.permanent-display");
		console_display = center.config().getConfigString("formatting.console-display");
		
	}

}
