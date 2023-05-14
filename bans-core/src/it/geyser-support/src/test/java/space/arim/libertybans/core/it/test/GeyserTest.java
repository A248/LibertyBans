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

package space.arim.libertybans.core.it.test;

import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.InstanceHolder;
import org.geysermc.floodgate.api.handshake.HandshakeHandlers;
import org.geysermc.floodgate.api.inject.PlatformInjector;
import org.geysermc.floodgate.api.link.PlayerLink;
import org.geysermc.floodgate.api.packet.PacketHandlers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.uuid.DynamicNameValidator;
import space.arim.libertybans.core.uuid.UUIDResolutionConfig;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GeyserTest {

	private static FloodgateApi floodgateApi;
	private final Configs configs;

	public GeyserTest(@Mock Configs configs) {
		this.configs = configs;
	}

	@BeforeAll
	public static void setFloodgateApi(@Mock FloodgateApi floodgateApi,
									   @Mock PlayerLink playerLink, @Mock PlatformInjector platformInjector,
									   @Mock PacketHandlers packetHandlers, @Mock HandshakeHandlers handshakeHandlers) {
		GeyserTest.floodgateApi = floodgateApi;
		assertTrue(InstanceHolder.set(
				floodgateApi, playerLink, platformInjector, packetHandlers, handshakeHandlers, UUID.randomUUID()
		));
	}

	private void setForcedPrefixOption(String forcedPrefix) {
		MainConfig mainConfig = mock(MainConfig.class);
		UUIDResolutionConfig uuidResolution = mock(UUIDResolutionConfig.class);
		when(configs.getMainConfig()).thenReturn(mainConfig);
		when(mainConfig.uuidResolution()).thenReturn(uuidResolution);
		when(uuidResolution.forceGeyserPrefix()).thenReturn(forcedPrefix);
	}

	@Test
	public void detectGeyserPrefix() {
		setForcedPrefixOption("");
		when(floodgateApi.getPlayerPrefix()).thenReturn("!");
		var validator = new DynamicNameValidator(configs);
		assertEquals("!", validator.associatedPrefix());
		assertEquals("!", validator.associatedPrefix());
	}

	@Test
	public void overrideDetectedGeyserPrefix() {
		setForcedPrefixOption("*");
		lenient().when(floodgateApi.getPlayerPrefix()).thenReturn("!");
		var validator = new DynamicNameValidator(configs);
		assertEquals("*", validator.associatedPrefix());
		assertEquals("*", validator.associatedPrefix());
	}

}
