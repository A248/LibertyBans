/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
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

package space.arim.libertybans.env.velocity.plugin;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.bootstrap.CulpritFinder;
import space.arim.libertybans.env.velocity.plugin.VelocityCulpritFinder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VelocityCulpritFinderTest {

	private static void setDescription(PluginContainer pluginMock, String id, String version) {
		PluginDescription description = mock(PluginDescription.class);
		when(description.getId()).thenReturn(id);
		when(description.getVersion()).thenAnswer((i) -> Optional.of(version));
		when(pluginMock.getDescription()).thenReturn(description);
	}

	@Test
	public void findCulprit(@Mock ProxyServer server, @Mock PluginManager pluginManager,
							@Mock PluginContainer pluginString, @Mock PluginContainer pluginOwnClass) {
		class OwnClass {}
		{
			when(server.getPluginManager()).thenReturn(pluginManager);

			when(pluginManager.getPlugins()).thenReturn(List.of(pluginString, pluginOwnClass));

			when(pluginString.getInstance()).thenAnswer((i) -> Optional.of("a".concat("b")));
			setDescription(pluginString, "JDK", "0");

			when(pluginOwnClass.getInstance()).thenAnswer((i) -> Optional.of(new OwnClass()));
			setDescription(pluginOwnClass, "Self", "1");
		}

		CulpritFinder culpritFinder = new VelocityCulpritFinder(server);
		assertEquals(Optional.of("JDK 0"), culpritFinder.findCulprit(List.class));
		assertEquals(Optional.of("Self 1"), culpritFinder.findCulprit(getClass()));
	}
}
