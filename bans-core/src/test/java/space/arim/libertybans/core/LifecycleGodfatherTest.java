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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.Test;
import space.arim.api.util.testing.InjectableConstructor;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LifecycleGodfatherTest {

	@Test
	public void allDeclared() {
		Set<Class<?>> classes;
		{
			Stream<Class<?>> unfilteredClasses;
			try (ScanResult scan = new ClassGraph()
					.enableClassInfo()
					.scan()) {
				unfilteredClasses = scan.getClassesImplementing(Part.class.getName())
						.getNames().stream()
						.map((className) -> {
							try {
								return Class.forName(className);
							} catch (ClassNotFoundException ex) {
								throw new RuntimeException(ex);
							}
						});
			}
			classes = unfilteredClasses.filter((clazz) -> {
				return clazz.isInterface() ||
						Set.of(clazz.getInterfaces()).contains(Part.class);
			}).collect(Collectors.toUnmodifiableSet());
		}
		new InjectableConstructor(LifecycleGodfather.class).verifyParametersContain(classes);
	}
}
