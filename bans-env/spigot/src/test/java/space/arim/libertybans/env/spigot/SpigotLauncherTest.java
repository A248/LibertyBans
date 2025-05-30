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
package space.arim.libertybans.env.spigot;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.bootstrap.Payload;
import space.arim.libertybans.bootstrap.PlatformId;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class SpigotLauncherTest {

	@TempDir
	public Path tempDir;

	@Test
	public void allBindings() {
		JavaPlugin plugin = MockJavaPlugin.create(tempDir);
		assertNotNull(new SpigotLauncher(new Payload<>(plugin, PlatformId.STUB, tempDir)).launch());
	}

}
