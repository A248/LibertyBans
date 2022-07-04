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

package space.arim.libertybans.it.executableunpack;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.bootstrap.DistributionMode;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;
import space.arim.libertybans.bootstrap.Platform;
import space.arim.libertybans.bootstrap.logger.Slf4jBootstrapLogger;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

public class UnpackTest {

	private static URL getJarURL(Class<?> clazz) {
		CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
		assertNotNull(codeSource, "No CodeSource");
		URL jar = codeSource.getLocation();
		assertNotNull(jar, "No CodeSource#getLocation");
		return jar;
	}

	private static Path getJarFile() {
		URL jar = getJarURL(LibertyBansLauncher.class);
		try {
			return Path.of(jar.toURI());
		} catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}
	}

	private ClassLoader unpack(Platform platform, ClassLoader parentClassLoader) {
		Path jarFile = getJarFile();
		LibertyBansLauncher launcher = new LibertyBansLauncher(
				new Slf4jBootstrapLogger(LoggerFactory.getLogger(getClass())),
				platform,
				Path.of("target/folder"),
				ForkJoinPool.commonPool(),
				jarFile);
		assertEquals(DistributionMode.JAR_OF_JARS, launcher.distributionMode());
		System.setProperty("libertybans.relocationbug.disablecheck", "true");
		CompletableFuture<ClassLoader> futureClassLoader = launcher.attemptLaunch(parentClassLoader);
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
		Platform platform = Platform.forCategory(Platform.Category.BUKKIT).build("it-unpack-and-link");
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
