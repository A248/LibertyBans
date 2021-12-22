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

package space.arim.libertybans.env.bungee;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.bootstrap.Platform;
import space.arim.libertybans.bootstrap.logger.BootstrapLogger;
import space.arim.libertybans.bootstrap.logger.Slf4jBootstrapLogger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BaseWrapperTest {

	@Test
	public void detectPlatform() {
		BootstrapLogger logger = new Slf4jBootstrapLogger(LoggerFactory.getLogger(getClass()));
		Platform platform = new BaseWrapper.Creator(MockPlugin.create(), logger).detectPlatform();
		assertTrue(platform.hasSlf4jSupport());
		assertFalse(platform.hasKyoriAdventureSupport());
	}
}
