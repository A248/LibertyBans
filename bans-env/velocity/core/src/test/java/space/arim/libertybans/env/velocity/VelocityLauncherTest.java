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

package space.arim.libertybans.env.velocity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static space.arim.libertybans.env.velocity.VelocityLauncher.extractMajorVer;

import java.nio.file.Path;

import com.velocitypowered.api.util.ProxyVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import space.arim.libertybans.bootstrap.Payload;
import space.arim.libertybans.bootstrap.PlatformId;

@ExtendWith(MockitoExtension.class)
public class VelocityLauncherTest {

	@TempDir
	public Path tempDir;

	@Test
	public void allBindings() {
		ProxyServer proxyServer = mock(ProxyServer.class);
		PluginContainer pluginContainer = mock(PluginContainer.class);
		when(proxyServer.getVersion()).thenReturn(new ProxyVersion("Velocity", getClass().getName(), "3.4.0"));

		assertNotNull(new VelocityLauncher(
				new Payload<>(pluginContainer, PlatformId.STUB, tempDir), proxyServer
		).launch());
	}

	@Test
	public void extractMajorVersion() {
		assertEquals(3, extractMajorVer("3.4.0"));
		assertEquals(3, extractMajorVer("3.3"));
		assertEquals(4, extractMajorVer("4"));
		assertEquals(3, extractMajorVer("3.4.0-b12"));
		assertEquals(3, extractMajorVer("3.3-af-preview"));
		assertEquals(4, extractMajorVer("4+45"));
		assertEquals(3, extractMajorVer("3.5.1 (git-4498f1e)"));
		assertEquals(2, extractMajorVer("2.5.0 (git-ab98f1e)"));
		assertEquals(4, extractMajorVer("4.2.0 (git-0b98f1e)"));
		assertEquals(3, extractMajorVer("3.5.1 (git-4498f1e-b15)"));
		assertEquals(2, extractMajorVer("2.5.0 (git-ab98f1e-b40)"));
		assertEquals(4, extractMajorVer("4.2.0 (git-0b98f1e-ba9)"));
	}

	@Test
	public void extractMajorVersionUnknown() {
		assertEquals(-1, extractMajorVer("<none>"));
		assertEquals(-1, extractMajorVer(""));
		assertEquals(-1, extractMajorVer("letters"));
		assertEquals(-1, extractMajorVer(".4"));
	}
}
