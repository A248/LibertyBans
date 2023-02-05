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
import java.util.Set;

public class DurationParser {

	private final Set<String> permanentArguments;
	
	public DurationParser(Set<String> permanentArguments) {
		this.permanentArguments = Set.copyOf(permanentArguments);
	}

	public DurationParser() {
		this(Set.of("perm"));
	}

	/**
	 * Parses a duration from an argument
	 *
	 * @param argument the argument
	 * @return the parsed duration, zero for permanent, a negative duration if unable to parse
	 */
	public Duration parse(String argument) {
		if (ContainsCI.containsIgnoreCase(permanentArguments, argument)) {
			return Duration.ZERO;
		}
		char[] characters = argument.toCharArray();
		int unitIndex = 0;
		for (int n = 0; n < characters.length; n++) {
			if (!Character.isDigit(characters[n])) {
				unitIndex = n;
				break;
			}
		}
		if (unitIndex == 0) {
			return Duration.ofNanos(-1L);
		}
		long number = Long.parseLong(argument.substring(0, unitIndex));
		ChronoUnit unit = switch (argument.substring(unitIndex)) {
			case "Y", "y" -> ChronoUnit.YEARS;
			case "MO", "mo" -> ChronoUnit.MONTHS;
			case "W", "w" -> ChronoUnit.WEEKS;
			case "D", "d" -> ChronoUnit.DAYS;
			case "H", "h" -> ChronoUnit.HOURS;
			case "M", "m" -> ChronoUnit.MINUTES;
			default -> null;
		};
		if (unit == null) {
			return Duration.ofNanos(-1L);
		}
		// Do not use Duration.of which does not accept estimated durations
		return unit.getDuration().multipliedBy(number);
	}

}
