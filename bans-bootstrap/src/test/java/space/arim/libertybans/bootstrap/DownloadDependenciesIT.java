/* 
 * LibertyBans-bootstrap
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-bootstrap is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-bootstrap is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-bootstrap. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.bootstrap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.slf4j.LoggerFactory;

import space.arim.libertybans.bootstrap.logger.BootstrapLogger;
import space.arim.libertybans.bootstrap.logger.Slf4jBootstrapLogger;

public class DownloadDependenciesIT {
	
	@TempDir
	public Path folder;
	
	private ExecutorService executor;
	
	@BeforeEach
	public void setup() {
		executor = Executors.newCachedThreadPool();
	}

	@ParameterizedTest
	@EnumSource(DependencyPlatform.class)
	public void testDownloadAllDependencies(DependencyPlatform platform) {
		BootstrapLogger logger = new Slf4jBootstrapLogger(LoggerFactory.getLogger(getClass()));
		LibertyBansLauncher launcher = new LibertyBansLauncher(logger, platform, folder, executor) {
			@Override
			protected void addUrlsToExternalClassLoader(ClassLoader apiClassLoader, Set<Path> paths) {
				for (Path path : paths) {
					logger.info("Downloaded to " + path.getFileName());
				}
			}
			@Override
			protected boolean skipSelfDependencies() {
				return true;
			}
		};
		ClassLoader classLoader = launcher.attemptLaunch().join();
		assertNotNull(classLoader, "Failed to download dependencies");
	}
	
	@AfterEach
	public void tearDown() throws InterruptedException {
		executor.shutdown();
		executor.awaitTermination(3, TimeUnit.SECONDS);
	}
	
}
