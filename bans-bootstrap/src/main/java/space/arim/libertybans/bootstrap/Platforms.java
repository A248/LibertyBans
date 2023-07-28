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

package space.arim.libertybans.bootstrap;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public final class Platforms {

	private Platforms() {}

	public static Platform.Builder bukkit() {
		return Platform.builderForCategory(Platform.Category.BUKKIT);
	}

	public static Platform.Builder bungeecord() {
		return Platform.builderForCategory(Platform.Category.BUNGEE);
	}

	public static Platform sponge(ClassLoader platformClassLoader) {
		return Platform
				.builderForCategory(Platform.Category.SPONGE)
				// Slf4j is an internal dependency
				.slf4jSupport(new LibraryDetection.ByClassLoaderScan(ProtectedLibrary.SLF4J_API, platformClassLoader))
				.kyoriAdventureSupport(LibraryDetection.enabled())
				.caffeineProvided(LibraryDetection.enabled())
				// HikariCP is an internal dependency
				.hiddenHikariCP(new LibraryDetection.ByClassLoaderScan(ProtectedLibrary.HIKARICP, platformClassLoader))
				.build("Sponge");
	}

	public static Platform velocity(ClassLoader platformClassLoader) {
		return Platform
				.builderForCategory(Platform.Category.VELOCITY)
				.slf4jSupport(LibraryDetection.enabled())
				.kyoriAdventureSupport(LibraryDetection.enabled())
				// Caffeine is an internal dependency
				.caffeineProvided(new LibraryDetection.ByClassLoaderScan(ProtectedLibrary.CAFFEINE, platformClassLoader))
				.jakartaProvided(new LibraryDetection.ByClassLoaderScan(ProtectedLibrary.JAKARTA_INJECT, platformClassLoader))
				.build("Velocity");
	}

	public static Platform standalone() {
		return Platform.builderForCategory(Platform.Category.STANDALONE).build("Standalone");
	}

	// Used for testing purposes
	public static Stream<Platform> allPossiblePlatforms(String platformName) {
		Set<Platform> platforms = new HashSet<>();
		for (Platform.Category category : Platform.Category.values()) {
			// Count from 0000 to 1111 in binary
			for (int setting = 0; setting < 0b10000; setting++) {
				final int flags = setting;
				platforms.add(Platform.builderForCategory(category)
						.kyoriAdventureSupport(() -> (flags & 0b0001) != 0)
						.slf4jSupport(() -> (flags & 0b0010) != 0)
						.caffeineProvided(() -> (flags & 0b0100) != 0)
						.jakartaProvided(() -> (flags & 0b1000) != 0)
						.build(platformName));
			}
		}
		return platforms.stream();
	}

}
