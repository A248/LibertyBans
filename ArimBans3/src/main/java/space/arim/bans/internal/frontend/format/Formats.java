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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import space.arim.bans.ArimBans;
import space.arim.bans.api.ArimBansLibrary;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.CommandType.Category;
import space.arim.bans.api.CommandType.SubCategory;
import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.api.exception.InvalidSubjectException;

import space.arim.api.util.StringsUtil;
import space.arim.api.uuid.PlayerNotFoundException;

public class Formats implements FormatsMaster {
	
	private final ArimBans center;
	
	private static final String DEFAULTING_TO_DATE_FORMAT = "Invalid date format specified! Defaulting to dd/MM/yyyy HH:mm:ss...";
	
	private SimpleDateFormat dateFormatter;
	
	private List<String> permanent_arguments;
	private final ConcurrentHashMap<PunishmentType, List<String>> layout = new ConcurrentHashMap<PunishmentType, List<String>>();
	private final ConcurrentHashMap<SubCategory, String> notification = new ConcurrentHashMap<SubCategory, String>();
	private Collection<String> blockForMuted;
	
	private boolean json = true;
	private String permanent_display;
	private String console_display;
	
	private final ConcurrentHashMap<TimeUnit, String> unitsFormat = new ConcurrentHashMap<TimeUnit, String>();
	private boolean time_enable_comma = true;

	public Formats(ArimBans center) {
		this.center = center;
		String invalid_string = ArimBansLibrary.INVALID_STRING_CODE;
		List<String> invalid_strings = Arrays.asList(invalid_string);
		permanent_arguments = invalid_strings;
		for (PunishmentType type : PunishmentType.values()) {
			layout.put(type, invalid_strings);
			notification.put(fromPunishmentType(type, true), invalid_string);
			if (type != PunishmentType.KICK) { 
				notification.put(fromPunishmentType(type, false), invalid_string);
			}
		}
		for (TimeUnit unit : TimeUnit.values()) {
			unitsFormat.put(unit, invalid_string);
		}
	}

	@Override
	public boolean useJson() {
		return json;
	}
	
	@Override
	public String formatMessageWithPunishment(String message, Punishment punishment) {
		long current = System.currentTimeMillis();
		return message.replace("%ID%", Integer.toString(punishment.id())).replace("%TYPE%", StringsUtil.capitaliseProperly(punishment.type().toString())).replace("%TARGET%", formatSubject(punishment.subject())).replace("%OPERATOR%", formatSubject(punishment.operator())).replace("%REASON%", punishment.reason()).replace("%DURATION%", formatTime(punishment.expiration() - punishment.date(), false)).replace("%TIME%", formatTime(punishment.expiration() - current, false)).replace("%EXPIRATION%", formatTime(punishment.expiration(), true)).replace("%DATE%", formatTime(punishment.date(), true));
	}
	
	@Override
	public String formatPunishment(Punishment punishment) {
		return formatMessageWithPunishment(StringsUtil.concat(layout.get(punishment.type()), '\n'), punishment);
	}
	
	@Override
	public String formatSubject(Subject subj) {
		switch (subj.getType()) {
		case PLAYER:
			try {
				return center.resolver().resolveUUID(subj.getUUID(), center.environment().isOnlineMode());
			} catch (PlayerNotFoundException ex) {
				throw new InvalidSubjectException("Subject's UUID " + subj.getUUID() + " could not be resolved to a name!", ex);
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
	public String formatNotification(Punishment punishment, boolean add, Subject operator) {
		return formatMessageWithPunishment(notification.get(fromPunishmentType(punishment.type(), add)), punishment).replace("%UNOPERATOR%", formatSubject(operator));
	}

	private enum TimeUnit {
		YEARS(31_536_000L),
		MONTHS(2_592_000L),
		WEEKS(604_800L),
		DAYS(86_400L),
		HOURS(3_600L),
		MINUTES(60L),
		SECONDS(1L);
		
		final long value;
		
		private TimeUnit(long value) {
			this.value = value * 1000_000L;
		}
		
		String varName() {
			return "%" + name() + "%";
		}
		
		String getKeyString() {
			return "time.units." + name().toLowerCase() + ".";
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
		
		StringBuilder builder = new StringBuilder();
		for (TimeUnit unit : TimeUnit.values()) {
			if (millis > unit.value && ArimBansLibrary.checkString(unitsFormat.get(unit))) {
				if (time_enable_comma) {
					builder.append(',').append(' ');
				}
				int value = (int) Math.floorDiv(millis, unit.value);
				builder.append(unitsFormat.get(unit).replace(unit.varName(), Integer.toString(value)));
			}
		}
		return (time_enable_comma) ? builder.toString().substring(2) : builder.toString();
	}
	
	@Override
	public long toMillis(String timespan) {
		long mult = 1_000_000;
		if (permanent_arguments.contains(timespan.toLowerCase())) {
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
	public boolean isCmdMuteBlocked(String command) {
		String cmd = command.split(" ")[0];
		cmd = (cmd.contains(":")) ? cmd.split(":")[1] : cmd.substring(0);
		return blockForMuted.contains(cmd);
	}
	
	@Override
	public void refreshConfig(boolean first) {
		
		try {
			dateFormatter = new SimpleDateFormat(center.config().getConfigString("formatting.dates"));
		} catch (IllegalArgumentException ex) {
			center.logs().log(Level.INFO, DEFAULTING_TO_DATE_FORMAT);
			dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		}
		
		dateFormatter.getCalendar().setTimeZone(TimeZone.getDefault());
		
		permanent_arguments = center.config().getConfigStrings("formatting.permanent-arguments");
		
		json = center.config().getConfigBoolean("formatting.use-json");
		permanent_display = center.config().getConfigString("formatting.permanent-display");
		console_display = center.config().getConfigString("formatting.console-display");
		blockForMuted = center.config().getConfigStrings("commands.block-for-muted");
		
	}
	
	// Exact same method as in CommandsMaster implementation
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
	public void refreshMessages(boolean first) {
		
		for (PunishmentType type : PunishmentType.values()) {
			String leadKeyAdd = center.config().keyString(type, Category.ADD);
			String leadKeyRemove = center.config().keyString(type, Category.REMOVE);
			SubCategory categoryAdd = fromPunishmentType(type, true);
			SubCategory categoryRemove = fromPunishmentType(type, false);
			layout.put(type, center.config().getMessagesStrings(leadKeyAdd + "layout"));
			notification.put(categoryAdd, center.config().getMessagesString(leadKeyAdd + "successful.notification"));
			if (type != PunishmentType.KICK) {
				notification.put(categoryRemove, center.config().getMessagesString(leadKeyRemove + "successful.notification"));
			}
		}
		
		for (TimeUnit unit : TimeUnit.values()) {
			if (center.config().getMessagesBoolean(unit.getKeyString() + "enable")) {
				unitsFormat.put(unit, center.config().getMessagesString(unit.getKeyString() + "message"));
			}
		}
		time_enable_comma = center.config().getMessagesBoolean("time.enable-comma");
		
	}

}
