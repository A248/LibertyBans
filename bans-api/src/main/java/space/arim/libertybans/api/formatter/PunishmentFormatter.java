/* 
 * LibertyBans-api
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-api. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.api.formatter;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.scope.ServerScope;

/**
 * Contains what little API exists for formatting punishments
 *
 */
public interface PunishmentFormatter {

	/**
	 * Formats a punishment type according to the configuration
	 * 
	 * @param type the punishment type to format
	 * @return the formatted string
	 */
	String formatPunishmentType(PunishmentType type);

	/**
	 * Formats a punishment type as a verb according to the configuration
	 *
	 * @param type the punishment type to format
	 * @return the formatted string
	 */
	String formatPunishmentTypeVerb(PunishmentType type);

	/**
	 * Gets the configured timezone
	 * 
	 * @return the zone id used for displaying dates
	 */
	ZoneId getTimezone();

	/**
	 * Gets the configured date time formatter used for displaying dates
	 * 
	 * @return the date time formatter
	 */
	DateTimeFormatter getDateTimeFormatter();

	/**
	 * Formats an absolute date in conjunction with {@link #getTimezone()} and
	 * {@link #getDateTimeFormatter()}. <br>
	 * <br>
	 * The maximum instant ({@link Instant#MAX}) may be passed to indicate a time so
	 * far in the future as to be considered the end of a permanent punishment.
	 * 
	 * @param date the date to format
	 * @return the formatted string
	 */
	String formatAbsoluteDate(Instant date);

	/**
	 * Formats a duration. A zero duration may be passed to represent an infinite or
	 * permanent duration.
	 * 
	 * @param duration the duration, or zero for a permanent duration
	 * @return the formatted string
	 */
	String formatDuration(Duration duration);

	/**
	 * Formats a server scope according to the configuration
	 * 
	 * @param scope the server scope
	 * @return the formatted string
	 */
	String formatScope(ServerScope scope);

}
