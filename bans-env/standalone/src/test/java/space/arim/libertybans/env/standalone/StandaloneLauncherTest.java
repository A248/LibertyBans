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

package space.arim.libertybans.env.standalone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.injector.Injector;
import space.arim.libertybans.api.LibertyBans;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.omnibus.DefaultOmnibus;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class StandaloneLauncherTest {

	private Path dataFolder;

	@BeforeEach
	public void setDataFolder(@TempDir Path dataFolder) {
		this.dataFolder = dataFolder;
	}

	@Test
	public void allBindings() {
		Injector injector = new StandaloneLauncher(
				dataFolder, new DefaultOmnibus()
		).createInjector(mock(ConsoleAudience.class));
		assertDoesNotThrow(() -> injector.request(LibertyBans.class));
		assertDoesNotThrow(() -> injector.request(BaseFoundation.class));
	}

	@Test
	public void startup() {
		ConsoleReceiver consoleReceiver = new ConsoleReceiver();
		Injector injector = new StandaloneLauncher(
				dataFolder, new DefaultOmnibus()
		).createInjector(consoleReceiver);

		// Startup
		BaseFoundation base = injector.request(BaseFoundation.class);
		assertDoesNotThrow(base::startup);

		injector.request(CommandDispatch.class).accept("about");
		assertFalse(consoleReceiver.messages().isEmpty());

		// Shutdown
		base.shutdown();
	}

}
