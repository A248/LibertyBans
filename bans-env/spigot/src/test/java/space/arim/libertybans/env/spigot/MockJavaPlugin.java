/* 
 * LibertyBans-env-spigot
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-spigot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-spigot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-spigot. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.spigot;

import org.bukkit.Server;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

public final class MockJavaPlugin extends JavaPlugin {

	private MockJavaPlugin(JavaPluginLoader loader, Path dataFolder) {
		super(loader, new PluginDescriptionFile("bareplugin", null, null),
				dataFolder.toFile(), dataFolder.resolve("plugin.jar").toFile());
	}

	public static JavaPlugin create(Path dataFolder) {
		return create(dataFolder, (server) -> {});
	}

	public static JavaPlugin create(Path dataFolder, Consumer<Server> configureMockServer) {
		Logger serverLogger = Logger.getLogger(MockJavaPlugin.class.getName());
		Server server = mock(Server.class);
		// Lenient because some Paper versions do not require this stub
		lenient().when(server.getLogger()).thenReturn(serverLogger);
		configureMockServer.accept(server);
		@SuppressWarnings("deprecation")
		JavaPluginLoader loader = new JavaPluginLoader(server);

		return new MockJavaPlugin(loader, dataFolder);
	}

}
