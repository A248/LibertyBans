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

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.bootstrap.logger.BootstrapLogger;
import space.arim.libertybans.bootstrap.logger.Slf4jBootstrapLogger;

import java.nio.file.Path;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DownloadDependenciesIT {
	
	@TempDir
	public Path folder;
	private final AtomicInteger folderUniqueifier = new AtomicInteger();

	@TestFactory
	public Stream<DynamicNode> downloadDependencies() {
		return Platform.Builder.allPossiblePlatforms("download test").map((platform) -> {
			return DynamicTest.dynamicTest("For platform " + platform, () -> runDownloadDependencies(platform));
		});
	}

	private void runDownloadDependencies(Platform platform) {
		Path subFolder = folder.resolve("" + folderUniqueifier.getAndIncrement());
		BootstrapLogger logger = new Slf4jBootstrapLogger(LoggerFactory.getLogger(getClass()));
		LibertyBansLauncher launcher = new LibertyBansLauncher(
				logger, platform, subFolder, ForkJoinPool.commonPool(), Path.of("jar")) {
			@Override
			public DistributionMode distributionMode() {
				return DistributionMode.TESTING;
			}
		};
		ClassLoader classLoader = launcher.attemptLaunch().join();
		assertNotNull(classLoader, "Failed to download dependencies");
	}
	
}
