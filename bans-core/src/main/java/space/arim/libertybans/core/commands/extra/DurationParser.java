/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.commands.extra;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationParser {

	private final Set<String> permanentArguments;

	/**
	 * Matches one "segment" of a duration, i.e. a number followed by a unit token.
	 * Units are tried longest-first in the alternation so that e.g. "mois" is not
	 * shadowed by "mo", and "min"/"sem"/"sec" are not shadowed by their english
	 * single-letter equivalents.
	 *
	 * Supported units:
	 *  years   : y, an
	 *  months  : mo, mois
	 *  weeks   : w, sem
	 *  days    : d, j
	 *  hours   : h            (identical in English and French)
	 *  minutes : m, min
	 *  seconds : s, sec
	 */
	private static final Pattern SEGMENT = Pattern.compile(
			"(\\d+)(mois|min|sem|sec|mo|an|w|y|d|j|h|m|s)", Pattern.CASE_INSENSITIVE);

	public DurationParser(Set<String> permanentArguments) {
		this.permanentArguments = Set.copyOf(permanentArguments);
	}

	public DurationParser() {
		this(Set.of("perm"));
	}

	/**
	 * Parses a duration from an argument. Accepts both English tokens (y, mo, w, d, h, m, s)
	 * and French tokens (an, mois, sem, j, h, min, sec), and allows chaining multiple
	 * segments together without spaces, e.g. "1j12h" or "2mo3sem".
	 *
	 * @param argument the argument
	 * @return the parsed duration, zero for permanent, a negative duration if unable to parse
	 */
	public Duration parse(String argument) {
		if (ContainsCI.containsIgnoreCase(permanentArguments, argument)) {
			return Duration.ZERO;
		}
		Matcher matcher = SEGMENT.matcher(argument);
		long totalNanos = 0L;
		int consumedUpTo = 0;
		boolean matchedAny = false;
		while (matcher.find()) {
			// Reject if there's a gap (unrecognized characters) between segments
			if (matcher.start() != consumedUpTo) {
				return Duration.ofNanos(-1L);
			}
			matchedAny = true;
			consumedUpTo = matcher.end();

			long number = Long.parseLong(matcher.group(1));
			ChronoUnit unit = unitFor(matcher.group(2));
			if (unit == null) {
				return Duration.ofNanos(-1L);
			}
			// Do not use Duration.of which does not accept estimated durations
			totalNanos += unit.getDuration().multipliedBy(number).toNanos();
		}
		if (!matchedAny || consumedUpTo != argument.length()) {
			return Duration.ofNanos(-1L);
		}
		return Duration.ofNanos(totalNanos);
	}

	private static ChronoUnit unitFor(String token) {
		return switch (token.toLowerCase(Locale.ROOT)) {
			case "y", "an" -> ChronoUnit.YEARS;
			case "mo", "mois" -> ChronoUnit.MONTHS;
			case "w", "sem" -> ChronoUnit.WEEKS;
			case "d", "j" -> ChronoUnit.DAYS;
			case "h" -> ChronoUnit.HOURS;
			case "m", "min" -> ChronoUnit.MINUTES;
			case "s", "sec" -> ChronoUnit.SECONDS;
			default -> null;
		};
	}

}
