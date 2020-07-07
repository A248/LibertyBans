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
package space.arim.libertybans.core.commands;

import static org.junit.jupiter.api.Assertions.*;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public abstract class CommandPackageTest {
	
	protected abstract CommandPackage getPackage(String command, String... args);
	
	@Test
	public void testNoArgs() {
		CommandPackage pkg = getPackage("cmd");
		assertEquals(pkg.getCommand(), "cmd");
		assertFalse(pkg.hasNext());
		assertTrue(pkg.allRemaining().isEmpty());
		try {
			pkg.next();
			fail("Should not be able to advanced past nonexistent argument");
		} catch (NoSuchElementException expected) {}
	}
	
	@Test
	public void testOneArgAndClone() {
		CommandPackage pkg = getPackage("cmd", "arg");
		assertTrue(pkg.hasNext());
		CommandPackage pkgClone = pkg.clone();
		assertEquals("arg", pkgClone.next());
		assertEquals("arg", pkg.allRemaining());
	}
	
	@Test
	public void testMultiArgConcat() {
		CommandPackage pkg = getPackage("cmd", "arg", "second", "another");
		assertEquals("arg second another", pkg.allRemaining());
	}
	
}
