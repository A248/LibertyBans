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

package space.arim.libertybans.it.test.applicable;

import jakarta.inject.Inject;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetAddressStrictness;
import space.arim.libertybans.it.util.RandomUtil;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(InjectionInvocationContextProvider.class)
public class WhichPunishmentIT {

	private final StrictnessAssertHelper assertHelper;
	private final PunishmentDrafter drafter;
	private final PunishmentSelector selector;

	@Inject
	public WhichPunishmentIT(StrictnessAssertHelper assertHelper,
							 PunishmentDrafter drafter, PunishmentSelector selector) {
		this.assertHelper = assertHelper;
		this.drafter = drafter;
		this.selector = selector;
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void selectPermanentRatherThanTemporaryPunishment() {
		UUID uuid = UUID.randomUUID();
		NetworkAddress address = RandomUtil.randomAddress();

		assertHelper.connectAndAssumeUnbannedUser(uuid, "User", address);

		Punishment permanentPunishment = drafter.draftBuilder()
				.type(PunishmentType.BAN)
				.victim(AddressVictim.of(address))
				.reason("No reason")
				.build().enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		Punishment temporaryPunishment = drafter.draftBuilder()
				.type(PunishmentType.BAN)
				.victim(PlayerVictim.of(uuid))
				.reason("Who said there needed to be a reason?")
				.duration(Duration.ofDays(1L))
				.build().enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);

		assertEquals(
				Optional.of(permanentPunishment),
				selector.getApplicablePunishment(uuid, address, PunishmentType.BAN).toCompletableFuture().join(),
				"Expected the permanent punishment. " +
						"Perhaps it was the temporary punishment #" + temporaryPunishment.getIdentifier()
		);
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void selectLongerLivedPunishment() {
		UUID uuid = UUID.randomUUID();
		NetworkAddress address = RandomUtil.randomAddress();

		assertHelper.connectAndAssumeUnbannedUser(uuid, "User", address);

		Punishment longerLastingPunishment = drafter.draftBuilder()
				.type(PunishmentType.BAN)
				.victim(PlayerVictim.of(uuid))
				.reason("I assure you there is no reason")
				.duration(Duration.ofDays(3L))
				.build().enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		Punishment shorterLivedPunishment = drafter.draftBuilder()
				.type(PunishmentType.BAN)
				.victim(AddressVictim.of(address))
				.reason("Other than being an arbitrary test subject")
				.duration(Duration.ofDays(1L))
				.build().enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);

		assertEquals(
				Optional.of(longerLastingPunishment),
				selector.getApplicablePunishment(uuid, address, PunishmentType.BAN).toCompletableFuture().join(),
				"Expected the longer-lasting punishment. " +
						"Perhaps it was the shorter lived punishment #" + shorterLivedPunishment.getIdentifier()
		);
	}
}
