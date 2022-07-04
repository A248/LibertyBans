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

package space.arim.libertybans.core;

import org.junit.jupiter.api.Test;
import space.arim.api.util.testing.InjectableConstructor;
import space.arim.libertybans.core.addon.Addon;

import java.util.Set;
import java.util.function.Predicate;

public class LifecycleGodfatherTest {

	@Test
	public void allDeclared() {
		Predicate<Class<?>> subClassFilter = (subClass) -> {
			return Set.of(subClass.getInterfaces()).contains(Part.class) && !subClass.equals(Addon.class);
		};
		new InjectableConstructor(LifecycleGodfather.class)
				.verifyParametersContainSubclassesOf(Part.class, subClassFilter);
	}

}
