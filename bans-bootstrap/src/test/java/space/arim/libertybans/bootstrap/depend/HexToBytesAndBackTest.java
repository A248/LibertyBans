/* 
 * LibertyBans-bootstrap
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-bootstrap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-bootstrap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-bootstrap. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.bootstrap.depend;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;

public class HexToBytesAndBackTest {

	private final byte[] bytes = randomBytes();
	
	private static byte[] randomBytes() {
		Random r = ThreadLocalRandom.current();
		byte[] array = new byte[r.nextInt(20)];
		r.nextBytes(array);
		return array;
	}
	
	@Test
	public void conversions() {
		String asHex = Dependency.bytesToHex(bytes);
		byte[] backToBytes = Dependency.hexStringToByteArray(asHex);
		assertArrayEquals(bytes, backToBytes);
		assertEquals(asHex, Dependency.bytesToHex(backToBytes));
	}
	
}
