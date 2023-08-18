/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

package space.arim.libertybans.it.env.platform;

import org.junit.jupiter.api.Test;
import space.arim.libertybans.core.env.InstanceType;
import space.arim.libertybans.it.PlatformSpecs;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class PlatformSpecsEqualityTest {

	private boolean equalityFor(String otherMethod) throws NoSuchMethodException {
		var thisClass = getClass();
		Method firstMethod = thisClass.getDeclaredMethod("value1");
		Method secondMethod = thisClass.getDeclaredMethod(otherMethod);
		PlatformSpecs firstAnnotation = firstMethod.getAnnotation(PlatformSpecs.class);
		PlatformSpecs secondAnnotation = secondMethod.getAnnotation(PlatformSpecs.class);
		return firstAnnotation.equals(secondAnnotation);
	}

	@Test
	public void isEqual() throws NoSuchMethodException {
		assertFalse(equalityFor("value2"));
		assertTrue(equalityFor("value3"));
		assertFalse(equalityFor("value4"));
	}

	@PlatformSpecs(instanceType = InstanceType.PROXY, pluginMessaging = false)
	void value1() {}
	@PlatformSpecs(instanceType = InstanceType.GAME_SERVER)
	void value2() {}
	@PlatformSpecs(instanceType = InstanceType.PROXY, pluginMessaging = false)
	void value3() {}
	@PlatformSpecs(instanceType = InstanceType.PROXY, pluginMessaging = true)
	void value4() {}

}
