/* 
 * LibertyBans-integrationtest
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-integrationtest is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-integrationtest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-integrationtest. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.it.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentBase;

public final class TestingUtil {

	private TestingUtil() {}
	
	/**
	 * Fast access to a {@code Random}
	 * 
	 * @return a random
	 */
	public static Random random() {
		return ThreadLocalRandom.current();
	}
	
	private static byte[] randomBytes(int length) {
		Random random = random();
		byte[] result = new byte[length];
		random.nextBytes(result);
		return result;
	}
	
	/**
	 * Random Ipv4 or Ipv6 address bytes
	 * 
	 * @return network address bytes
	 */
	public static byte[] randomAddress() {
		return randomBytes((random().nextBoolean()) ? 4 : 16);
	}
	
	/**
	 * Asserts the qualities of the punishment objects are equal
	 * 
	 * @param expected expected value
	 * @param actual actualv alue
	 */
	public static void assertEqualDetails(PunishmentBase expected, PunishmentBase actual) {
		assertEquals(expected.getType(), actual.getType());
		assertEquals(expected.getVictim(), actual.getVictim());
		assertEquals(expected.getOperator(), actual.getOperator());
		assertEquals(expected.getReason(), actual.getReason());
		assertEquals(expected.getScope(), actual.getScope());
		assertEquals(expected.getStart(), actual.getStart());
		assertEquals(expected.getEnd(), actual.getEnd());
		if (expected instanceof Punishment && actual instanceof Punishment) {
			assertEquals(((Punishment) expected).getID(), ((Punishment) actual).getID());
		}
	}

	/**
	 * Generates a random string
	 * 
	 * @param maxLength the maximum length of the string
	 * @return the random string
	 */
	public static String randomString(int maxLength) {
		if (maxLength == 0) {
			return "";
		}
		Random random = random();
		final String possibleChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		int length = (maxLength == 1) ? 1 : random.nextInt(maxLength - 1);

		StringBuilder result = new StringBuilder();
		for (int n = 0; n < length; n++) {
			int index = random.nextInt(possibleChars.length());
			result.append(possibleChars.charAt(index));
		}
		return result.toString();
	}
	
}
