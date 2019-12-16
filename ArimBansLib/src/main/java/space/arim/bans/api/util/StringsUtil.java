/* 
 * ArimBansLib, an API for ArimBans
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansLib. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api.util;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import space.arim.registry.util.ThreadLocalSupplier;

public final class StringsUtil {
	
	private static final ThreadLocalSupplier<SimpleDateFormat> BASIC_DATE_FORMATTER = new ThreadLocalSupplier<SimpleDateFormat>(() -> new SimpleDateFormat(""));
	
	private StringsUtil() {}
	
	/**
	 * Utilises class <code>com.google.common.net.InetAddresses</code> <br>
	 * <br>
	 * <b>If that class is not on the classpath do not call this method!</b>
	 * 
	 * @param address - the address to validate
	 * @return true if valid, false otherwise
	 */
	public static boolean validAddress(String address) {
		return com.google.common.net.InetAddresses.isInetAddress(address);
	}
	
	public static String capitaliseProperly(String input) {
		Objects.requireNonNull(input, "Input string not be null!");
		return (input.length() == 1) ? input.toUpperCase() : Character.toUpperCase(input.charAt(0)) + input.substring(1);
	}
	
	public static String[] chopOffOne(String[] input) {
		Objects.requireNonNull(input, "Input array must not be null!");
		return Arrays.copyOfRange(input, 1, input.length);
	}
	
	public static String concat(List<String> input, char separator) {
		return concat(Objects.requireNonNull(input, "Input list must not be null!").toArray(new String[] {}), separator);
	}
	
	public static String concat(String[] input, char separator) {
		StringBuilder builder = new StringBuilder();
		for (String m : Objects.requireNonNull(input, "Input array must not be null!")) {
			if (m != null && !m.isEmpty()) {
				builder.append(separator).append(m);
			}
		}
		return (builder.length() == 0) ? "" : builder.toString().substring(1);
	}
	
	public static String basicTodaysDate() {
		return BASIC_DATE_FORMATTER.get().format(new Date());
	}
	
}
