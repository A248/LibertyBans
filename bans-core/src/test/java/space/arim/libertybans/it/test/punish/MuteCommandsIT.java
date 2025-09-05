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

package space.arim.libertybans.it.test.punish;

import jakarta.inject.Inject;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.env.platform.QuackPlatform;
import space.arim.libertybans.it.env.platform.QuackPlayer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(InjectionInvocationContextProvider.class)
public class MuteCommandsIT {

	private final QuackPlatform platform;

	@Inject
	public MuteCommandsIT(QuackPlatform platform) {
        this.platform = platform;
	}

	@TestTemplate
	@Inject
	public void muteCommandNotMuted(Guardian guardian) {
        QuackPlayer player = platform.newPlayer().buildFullyRandom();
        platform.assumeLogin(player);
		assertNull(guardian.checkChat(player.getUniqueId(), player.getAddress(), "msg").join());
	}

	@TestTemplate
	@Inject
	public void blockMuteCommandsCorrectly(Guardian guardian, PunishmentDrafter drafter) {
        QuackPlayer player = platform.newPlayer().buildFullyRandom();
        platform.assumeLogin(player);
        UUID uuid = player.getUniqueId();
        NetworkAddress address = player.getNetworkAddress();

		drafter.draftBuilder()
				.type(PunishmentType.MUTE)
				.victim(PlayerVictim.of(uuid))
				.reason("why not")
				.build()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		assertNotNull(guardian.checkChat(uuid, address, "msg").join());
		assertNotNull(guardian.checkChat(uuid, address, "someplugin:msg").join());
		assertNotNull(guardian.checkChat(uuid, address, "clan chat hello my clan").join());
		assertNull(guardian.checkChat(uuid, address, "clan another clan command not block").join());
		assertNull(guardian.checkChat(uuid, address, "notblocked").join());
	}

}
