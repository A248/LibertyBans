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
package space.arim.libertybans.core.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class VendorTest {

	// Map.of() is not ordered
	private static <K, V> Map<K, V> orderedMap(K key1, V value1, K key2, V value2) {
		LinkedHashMap<K, V> linkedMap = new LinkedHashMap<>();
		linkedMap.put(key1, value1);
		linkedMap.put(key2, value2);
		return linkedMap;
	}

	@Test
	public void testFormatProperties() {
		assertEquals("?autocommit=false",
				Vendor.MARIADB.formatConnectionProperties(Map.of("autocommit", false)));
		assertEquals("?autocommit=false&defaultFetchSize=100",
				Vendor.MARIADB.formatConnectionProperties(orderedMap("autocommit", false, "defaultFetchSize", 100)));
		assertEquals(";sql.enforce_types=true;hsqldb.tx_interrupt_rollback=true",
				Vendor.HSQLDB.formatConnectionProperties(orderedMap("sql.enforce_types", true, "hsqldb.tx_interrupt_rollback", true)));
		assertEquals(";sql.enforce_types=true",
				Vendor.HSQLDB.formatConnectionProperties((Map.of("sql.enforce_types", true))));
	}

}
