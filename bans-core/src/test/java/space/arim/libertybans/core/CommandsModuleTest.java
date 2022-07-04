/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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
import space.arim.api.util.testing.SubClassesOf;
import space.arim.libertybans.core.commands.SubCommandGroup;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandsModuleTest {

	@Test
	public void allCommandsDeclared() {

		Set<Class<?>> subCommandClasses = new HashSet<>(
				new SubClassesOf(SubCommandGroup.class).scan(
						SubCommandGroup.class.getModule(), (cls) -> !Modifier.isAbstract(cls.getModifiers())
				));
		for (Method method : CommandsModule.class.getMethods()) {
			if (method.getDeclaringClass().equals(Object.class)) {
				continue;
			}
			Class<?> command = method.getParameters()[0].getType();
			assertTrue(SubCommandGroup.class.isAssignableFrom(command), () -> "Parameter " + command + " is not a sub-command");
			assertTrue(subCommandClasses.remove(command), () -> "Sub-command " + command + " declared more than once");
		}
		assertTrue(subCommandClasses.isEmpty(), () -> "These sub-commands were not declared: " + subCommandClasses);
	}

}
