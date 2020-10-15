/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.config;

import java.time.format.DateTimeFormatter;

/**
 * {@link DateTimeFormatter} does not retain the pattern string it was created with,
 * therefore this wrapper is required for reserialisation. <br>
 * <br>
 * <i>This class is public so that it may be used in dynamic proxies</i>
 * 
 * @author A248
 *
 */
public class DateTimeFormatterWithPattern {

	private final String pattern;
	private transient final DateTimeFormatter formatter;
	
	/**
	 * Creates from a pattern string
	 * 
	 * @param pattern the pattern string
	 * @throws IllegalArgumentException if the pattern is not valid
	 */
	DateTimeFormatterWithPattern(String pattern) {
		this.pattern = pattern;
		formatter = DateTimeFormatter.ofPattern(pattern);
	}
	
	String getPattern() {
		return pattern;
	}
	
	DateTimeFormatter getFormatter() {
		return formatter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + pattern.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DateTimeFormatterWithPattern)) {
			return false;
		}
		DateTimeFormatterWithPattern other = (DateTimeFormatterWithPattern) object;
		return pattern.equals(other.pattern);
	}

	@Override
	public String toString() {
		return "DateTimeFormatterWithPattern [pattern=" + pattern + "]";
	}
	
}
