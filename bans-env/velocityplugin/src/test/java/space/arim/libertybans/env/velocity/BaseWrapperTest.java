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

package space.arim.libertybans.env.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.bootstrap.Platform;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class BaseWrapperTest {

	@TempDir
	public Path dataFolder;

	@Test
	public void detectPlatform() {
		VelocityPlugin plugin = new VelocityPlugin(
				mock(ProxyServer.class), dataFolder, LoggerFactory.getLogger(getClass()));
		Platform platform = new BaseWrapper.Creator(plugin).detectPlatform();
		assertTrue(platform.hasSlf4jSupport());
		assertTrue(platform.hasKyoriAdventureSupport());
	}
}
