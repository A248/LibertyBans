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

package space.arim.libertybans.it.test.select;

import jakarta.inject.Inject;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.core.selector.InternalSelector;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.test.applicable.StrictnessAssertHelper;
import space.arim.libertybans.it.util.RandomUtil;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(InjectionInvocationContextProvider.class)
public class SelectorIT {

	private final StrictnessAssertHelper assertHelper;

	@Inject
	public SelectorIT(StrictnessAssertHelper assertHelper) {
		this.assertHelper = assertHelper;
	}

	@TestTemplate
	public void selectMute(InternalSelector selector, PunishmentDrafter drafter) {
		UUID uuid = UUID.randomUUID();
		NetworkAddress address = RandomUtil.randomAddress();
		assertHelper.connectAndAssumeUnbannedUser(uuid, "name", address);

		Punishment punishment = drafter.draftBuilder()
				.type(PunishmentType.MUTE)
				.victim(PlayerVictim.of(uuid))
				.reason("why not")
				.build()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		assertEquals(
				punishment,
				selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE)
						.toCompletableFuture().join().orElse(null)
		);
	}
}
