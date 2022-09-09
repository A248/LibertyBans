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

package space.arim.libertybans.it.executableunpack;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import space.arim.libertybans.bootstrap.DistributionMode;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;
import space.arim.libertybans.bootstrap.Platform;
import space.arim.libertybans.bootstrap.Platforms;
import space.arim.libertybans.bootstrap.logger.JulBootstrapLogger;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class UnpackTest {

	private Path folder;

	@BeforeEach
	public void setFolder(@TempDir Path folder) {
		this.folder = folder;
	}

	private ClassLoader unpack(Platform platform, ClassLoader parentClassLoader) {
		LibertyBansLauncher launcher = new LibertyBansLauncher.Builder()
				.folder(folder)
				.logger(new JulBootstrapLogger(Logger.getLogger(getClass().getName())))
				.platform(platform)
				.executor(ForkJoinPool.commonPool())
				.distributionMode(DistributionMode.JAR_OF_JARS)
				.parentLoader(parentClassLoader)
				.build();
		System.setProperty("libertybans.relocationbug.disablecheck", "true");
		CompletableFuture<ClassLoader> futureClassLoader = launcher.attemptLaunch();
		futureClassLoader.orTimeout(5L, TimeUnit.SECONDS);
		return assertDoesNotThrow(futureClassLoader::join);
	}

	@ParameterizedTest
	@ArgumentsSource(PlatformProvider.class)
	public void unpack(Platform platform) {
		assertDoesNotThrow(() -> unpack(platform, getClass().getClassLoader()));
	}

	@Test
	public void unpackAndLink() {
		ClassLoader isolatedParentClassLoader = new ClassLoader("EmptyCL", ClassLoader.getSystemClassLoader()) {};
		Platform platform = Platforms.bukkit().build("it-unpack-and-link");
		ClassLoader classLoader = unpack(platform, isolatedParentClassLoader);

		List<String> classNames;
		try (ScanResult scanResult = new ClassGraph()
				.overrideClassLoaders(classLoader)
				.enableClassInfo()
				.ignoreClassVisibility()
				.scan()) {
			classNames = scanResult.getAllClasses()
					.filter(classInfo -> {
						String className = classInfo.getName();
						return className.startsWith("space.arim.libertybans")
								&& !className.startsWith("space.arim.libertybans.env");
					})
					.getNames();
		}
		assertNotEquals(List.of(), classNames, "Found no classes");
		classNames.forEach((className) -> {
			try {
				Class.forName(className, true, classLoader);
			} catch (ClassNotFoundException | LinkageError ex) {
				fail("Unable to find or link class " + className, ex);
			}
		});
	}

}
