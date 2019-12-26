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
package space.arim.api.util;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public final class StringsUtil {
	
	private static final ThreadLocal<SimpleDateFormat> BASIC_DATE_FORMATTER = ThreadLocal.withInitial(() -> new SimpleDateFormat("dd-MM-yyyy"));
	
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
		return (input == null) ? input : (input.length() == 1) ? input.toUpperCase() : Character.toUpperCase(input.charAt(0)) + input.substring(1);
	}
	
	public static <T> T[] chopOffOne(T[] input) {
		return copyRange(input, 1, input.length);
	}
	
	private static <T> T[] copyRange(T[] input, int start, int end) {
		return Arrays.copyOfRange(Objects.requireNonNull(input, "Input array must not be null!"), start, end);
	}
	
	public static String concatRange(List<String> input, char separator, int start, int end) {
		return concatRange(Objects.requireNonNull(input, "Input list must not be null!").toArray(new String[] {}), separator, start, end);
	}
	
	public static String concatRange(String[] input, char separator, int start, int end) {
		StringBuilder builder = new StringBuilder();
		String[] selected = copyRange(Objects.requireNonNull(input, "Input array must not be null!"), start, end);
		for (String m : selected) {
			if (m != null && !m.isEmpty()) {
				builder.append(separator).append(m);
			}
		}
		return builder.length() == 0 ? "" : builder.toString().substring(1);
	}
	
	public static String concat(List<String> input, char separator) {
		return concatRange(input, separator, 0, input.size());
	}
	
	public static String concat(String[] input, char separator) {
		return concatRange(input, separator, 0, input.length);
	}
	
	public static String basicTodaysDate() {
		return BASIC_DATE_FORMATTER.get().format(new Date());
	}
	
}
