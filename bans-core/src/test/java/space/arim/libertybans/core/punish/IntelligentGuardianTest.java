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

package space.arim.libertybans.core.punish;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.core.config.Configs;
import space.arim.libertybans.core.config.InternalFormatter;
import space.arim.libertybans.core.config.MainConfig;
import space.arim.libertybans.core.selector.EnforcementConfig;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.core.selector.IntelligentGuardian;
import space.arim.libertybans.core.selector.InternalSelector;
import space.arim.libertybans.core.selector.cache.MuteCache;
import space.arim.libertybans.core.uuid.UUIDManager;
import space.arim.libertybans.it.util.RandomUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IntelligentGuardianTest {

	private final FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();
	private final MuteCache muteCache;

	private UUID uuid;
	private NetworkAddress address;
	private Guardian guardian;

	public IntelligentGuardianTest(@Mock MuteCache muteCache) {
		this.muteCache = muteCache;
	}

	@BeforeEach
	public void setup(@Mock Configs configs, @Mock ScopeManager scopeManager, @Mock InternalFormatter formatter,
					  @Mock InternalSelector selector, @Mock UUIDManager uuidManager) {
		uuid = UUID.randomUUID();
		address = RandomUtil.randomAddress();

		guardian = new IntelligentGuardian(configs, futuresFactory, scopeManager, formatter, selector, uuidManager, muteCache);

		MainConfig mainConfig = mock(MainConfig.class);
		EnforcementConfig enforcementConfig = mock(EnforcementConfig.class);
		lenient().when(configs.getMainConfig()).thenReturn(mainConfig);
		lenient().when(mainConfig.enforcement()).thenReturn(enforcementConfig);
		lenient().when(enforcementConfig.muteCommands()).thenReturn(Set.of("msg", "whisper"));
	}

	private <T> CentralisedFuture<T> completedFuture(T value) {
		return futuresFactory.completedFuture(value);
	}

	@Test
	public void checkChatNotMuted() {
		when(muteCache.getCachedMuteMessage(uuid, address)).thenReturn(completedFuture(Optional.empty()));

		assertNull(guardian.checkChat(uuid, address, null).join());
		assertNull(guardian.checkChat(uuid, address, "help").join());
		assertNull(guardian.checkChat(uuid, address, "msg").join());
	}

	@Test
	public void checkChatIsMuted(@Mock Punishment punishment) {
		Component denyMessage = Component.text("You are forbidden to chat");
		when(muteCache.getCachedMuteMessage(uuid, address)).thenReturn(completedFuture(Optional.of(denyMessage)));

		assertEquals(denyMessage, guardian.checkChat(uuid, address, null).join());
		assertNull(guardian.checkChat(uuid, address, "help").join(),
				"The /help command is not blocked from muted players");
		assertEquals(denyMessage, guardian.checkChat(uuid, address, "msg").join(),
				"The /msg command is indeed blocked from muted players");
		assertEquals(denyMessage, guardian.checkChat(uuid, address, "msg Player1 hi").join(),
				"ibid");
	}
}
