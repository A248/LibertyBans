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
package space.arim.libertybans.it.util;

import space.arim.libertybans.api.NetworkAddress;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomUtil {

	private RandomUtil() {}

	private static Random random() {
		return ThreadLocalRandom.current();
	}

	/**
	 * Generates random bytes of the specified length
	 *
	 * @param length the length
	 * @return the random bytes
	 */
	public static byte[] randomBytes(int length) {
		byte[] result = new byte[length];
		random().nextBytes(result);
		return result;
	}

	public static boolean randomBoolean() {
		return random().nextBoolean();
	}

	/**
	 * Random Ipv4 or Ipv6 address bytes
	 * 
	 * @return network address bytes
	 */
	private static byte[] randomAddressBytes() {
		return randomBytes((random().nextBoolean()) ? 4 : 16);
	}
	
	/**
	 * Random Ipv4 or Ipv6 InetAddress
	 * 
	 * @return the network address 
	 */
	public static NetworkAddress randomAddress() {
		return NetworkAddress.of(randomAddressBytes());
	}

	/**
	 * Random player username
	 *
	 * @return the name
	 */
	public static String randomName() {
		// Names too small tend to be in use
		return RandomUtil.randomString(12, 16);
	}

	/**
	 * Generates a random string
	 * 
	 * @param maxLength the maximum length of the string
	 * @return the random string
	 */
	public static String randomString(int maxLength) {
		return randomString(0, maxLength);
	}

	/**
	 * Generates a random string
	 * 
	 * @param minLength the minimum length of the string
	 * @param maxLength the maximum length of the string
	 * @return the random string
	 */
	public static String randomString(int minLength, int maxLength) {
		if (minLength > maxLength || minLength < 0) {
			throw new IllegalArgumentException("Bad min and max length: min " + minLength + " and max " + maxLength);
		}
		if (maxLength == minLength) {
			return randomStringFromLength(maxLength);
		}
		int length = minLength + random().nextInt(1 + maxLength - minLength);
		return randomStringFromLength(length);
	}
	
	private static String randomStringFromLength(int length) {
		if (length == 0) {
			return "";
		}
		final String possibleChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		Random random = random();

		StringBuilder result = new StringBuilder();
		for (int n = 0; n < length; n++) {
			int index = random.nextInt(possibleChars.length());
			result.append(possibleChars.charAt(index));
		}
		return result.toString();
	}

}
