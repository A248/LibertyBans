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

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

public final class MockPlugin extends Plugin {

	private MockPlugin(ProxyServer server, PluginDescription description) {
		super(server, description);
	}

	public static Plugin create() {
		return create((server) -> {});
	}

	public static Plugin create(Consumer<ProxyServer> mockServerConfigure) {
		ProxyServer server = mock(ProxyServer.class);
		// Lenient because Waterfall does not require this stub
		lenient().when(server.getLogger()).thenReturn(Logger.getLogger(MockPlugin.class.getName()));
		mockServerConfigure.accept(server);
		PluginDescription description = new PluginDescription();
		description.setName("MockPlugin"); // Required on Waterfall
		Plugin plugin = new MockPlugin(server, description);
		// BungeeCord is so weird that it does not initialize the fields in Plugin
		// Manually calling the package-private Plugin.init method is the only solution
		try {
			Method method = Plugin.class.getDeclaredMethod("init", ProxyServer.class, PluginDescription.class);
			method.setAccessible(true); // BungeeCord won't use JPMS in a million years
			method.invoke(plugin, server, description);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
			throw new RuntimeException(ex);
		}
		return plugin;
	}
}
