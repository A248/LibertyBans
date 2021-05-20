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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.bootstrap.DistributionMode;
import space.arim.libertybans.bootstrap.LibertyBansLauncher;
import space.arim.libertybans.bootstrap.Platforms;
import space.arim.libertybans.bootstrap.logger.Slf4jBootstrapLogger;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class UnpackTest {

	private Path getJarFile() {
		CodeSource codeSource = LibertyBansLauncher.class.getProtectionDomain().getCodeSource();
		assertNotNull(codeSource, "No CodeSource");
		URL jar = codeSource.getLocation();
		assertNotNull(jar, "No CodeSource#getLocation");
		try {
			return Path.of(jar.toURI());
		} catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Test
	public void unpack() {
		Path jarFile = getJarFile();
		LibertyBansLauncher launcher = new LibertyBansLauncher(
				new Slf4jBootstrapLogger(LoggerFactory.getLogger(getClass())),
				Platforms.velocity(), // Supports slf4j and adventure
				Path.of("target/folder"),
				ForkJoinPool.commonPool(),
				jarFile);
		assertEquals(DistributionMode.JAR_OF_JARS, launcher.distributionMode());
		CompletableFuture<ClassLoader> futureClassLoader = launcher.attemptLaunch();
		futureClassLoader.orTimeout(5L, TimeUnit.SECONDS);
		ClassLoader classLoader = assertDoesNotThrow(futureClassLoader::join);
		try {
			Class<?> databaseSettings = classLoader.loadClass("space.arim.libertybans.core.database.DatabaseSettings");
			Class<?> databaseManager = classLoader.loadClass("space.arim.libertybans.core.database.DatabaseManager");
			databaseSettings.getConstructor(databaseManager).newInstance((Object) null);
		} catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
				| IllegalAccessException | InvocationTargetException ex) {
			throw Assertions.<RuntimeException>fail("Could not load, initialize, or use database-related classes", ex);
		}
	}

}
