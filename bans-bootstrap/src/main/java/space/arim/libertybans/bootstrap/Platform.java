/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

import space.arim.libertybans.bootstrap.logger.BootstrapLogger;

import java.util.*;
import java.util.stream.Stream;

public final class Platform {

	final Category category;
	final PlatformId platformId;
	private final BootstrapLogger logger;
	private final Map<DependencyBundle, LibraryDetection> libraryDetectionMap;
	private final LibraryDetection hikariCP;

	Platform(Category category, PlatformId platformId, BootstrapLogger logger,
             Map<DependencyBundle, LibraryDetection> libraryDetectionMap, LibraryDetection hikariCP) {
        this.category = category;
        this.platformId = Objects.requireNonNull(platformId, "platformId");
		this.logger = logger;
        this.libraryDetectionMap = libraryDetectionMap;
        this.hikariCP = hikariCP;
	}

	public boolean isBundleProvided(DependencyBundle bundle) {
		LibraryDetection detection = libraryDetectionMap.get(bundle);
		boolean provided = detection != null && detection.evaluatePresence(logger);
		if (provided) {
			logger.debug("Found provided dependency bundle: " + bundle);
		}
		return provided;
	}

	boolean hasHiddenHikariCP() {
		return hikariCP != null && hikariCP.evaluatePresence(logger);
	}

	public static PreBuilder builder(Category category) {
		return new PreBuilder(category);
	}

	public static final class PreBuilder {

		private final Category category;

        PreBuilder(Category category) {
            this.category = Objects.requireNonNull(category, "category");
        }

        public Builder nameAndVersion(String platformName, String platformVersion) {
			return new Builder(category, platformName, platformVersion);
		}
	}

	public static final class Builder {

		private final Category category;
		private final String platformName;
		private final String platformVersion;
		private final Map<DependencyBundle, LibraryDetection> libraryDetectionMap = new EnumMap<>(DependencyBundle.class);
		private LibraryDetection hikariCP;

		private Builder(Category category, String platformName, String platformVersion) {
			this.category = category;
            this.platformName = platformName;
            this.platformVersion = platformVersion;
        }

		public Builder slf4jSupport(LibraryDetection slf4j) {
			libraryDetectionMap.put(DependencyBundle.SLF4J, slf4j);
			return this;
		}

		public Builder kyoriAdventureSupport(LibraryDetection kyoriAdventure) {
			libraryDetectionMap.put(DependencyBundle.KYORI, kyoriAdventure);
			return this;
		}

		public Builder caffeineProvided(LibraryDetection caffeine) {
			libraryDetectionMap.put(DependencyBundle.CAFFEINE, caffeine);
			return this;
		}

		public Builder jakartaProvided(LibraryDetection jakarta) {
			libraryDetectionMap.put(DependencyBundle.JAKARTA, jakarta);
			return this;
		}

		public Builder snakeYamlProvided(LibraryDetection snakeYaml) {
			libraryDetectionMap.put(DependencyBundle.SNAKEYAML, snakeYaml);
			return this;
		}

		public Builder hiddenHikariCP(LibraryDetection hikariCP) {
			this.hikariCP = hikariCP;
			return this;
		}

		public Platform build(BootstrapLogger logger) {
			/*
			We want to distinguish between platform category (e.g. Bukkit) and specific brand (e.g., Paper)
			To do that, check both pieces of information, and if they're different, include both of them.
			 */
			PlatformId platformId;
			if (category.name().equalsIgnoreCase(platformName)) {
				platformId = new PlatformId(platformName, platformVersion);
			} else {
				platformId = new PlatformId(platformName + " (" + category.display() + ')', platformVersion);
			}
			return new Platform(category, platformId, logger, libraryDetectionMap, hikariCP);
		}
	}

	public enum Category {
		BUKKIT,
		BUNGEECORD,
        FABRIC,
		SPONGE,
		VELOCITY,
		STANDALONE;

		String display() {
			String categoryName = name();
			return categoryName.charAt(0) + categoryName.substring(1).toLowerCase(Locale.ROOT);
		}
	}

	@Override
	public String toString() {
		return "Platform{" + platformId + '}';
	}

	public static Stream<Builder> allPossiblePlatforms(String platformName) {
		Set<Builder> platforms = new HashSet<>();
		for (Platform.Category category : Platform.Category.values()) {
			// Count from 00000 to 11111 in binary
			for (int setting = 0; setting < 0b100000; setting++) {
				final int flags = setting;
				platforms.add(Platform.builder(category)
						.nameAndVersion(platformName, "0.0")
						.kyoriAdventureSupport((l) -> (flags & 0b0001) != 0)
						.slf4jSupport((l) -> (flags & 0b0010) != 0)
						.caffeineProvided((l) -> (flags & 0b0100) != 0)
						.jakartaProvided((l) -> (flags & 0b1000) != 0)
						.snakeYamlProvided((l) -> (flags & 0b10000) != 0));
			}
		}
		return platforms.stream();
	}

}
