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

package space.arim.libertybans.it.test.applicable;

import jakarta.inject.Inject;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.select.AddressStrictness;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.api.select.SortPunishments;
import space.arim.libertybans.core.service.SettableTime;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.IrrelevantData;
import space.arim.libertybans.it.SetAddressStrictness;
import space.arim.libertybans.it.resolver.RandomOperatorResolver;
import space.arim.libertybans.it.util.RandomUtil;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static space.arim.libertybans.api.PunishmentType.BAN;
import static space.arim.libertybans.api.PunishmentType.MUTE;
import static space.arim.libertybans.api.PunishmentType.WARN;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith(RandomOperatorResolver.class)
public class MultipleApplicableIT {

	private final PunishmentDrafter drafter;
	private final PunishmentSelector selector;
	private final StrictnessAssertHelper strictnessAssertHelper;
	private final SettableTime time;
	private final Operator operator;

	@Inject
	public MultipleApplicableIT(PunishmentDrafter drafter, PunishmentSelector selector,
								StrictnessAssertHelper strictnessAssertHelper, SettableTime time,
								@DontInject Operator operator) {
		this.drafter = drafter;
		this.selector = selector;
		this.strictnessAssertHelper = strictnessAssertHelper;
		this.time = time;
		this.operator = operator;
	}

	private Punishment addPunishment(PunishmentType type, Victim victim, String reason, Duration time) {
		return drafter.draftBuilder()
				.type(type)
				.victim(victim)
				.operator(operator)
				.reason(reason)
				.duration(time)
				.build()
				.enactPunishment()
				.toCompletableFuture()
				.join()
				.orElseThrow(AssertionError::new);
	}

	@TestTemplate
	@SetAddressStrictness(AddressStrictness.NORMAL)
	@IrrelevantData
	public void selectHistory() {
		UUID uuid = UUID.randomUUID();
		NetworkAddress address = RandomUtil.randomAddress();
		strictnessAssertHelper.connectAndAssertUnbannedUser(uuid, "username", address);

		Punishment banOnUuid = addPunishment(BAN, PlayerVictim.of(uuid), "ban on uuid", Duration.ZERO);
		Punishment banOnAddress = addPunishment(BAN, AddressVictim.of(address), "ban on address", Duration.ZERO);
		Punishment banOnComposite = addPunishment(BAN, CompositeVictim.of(uuid, address), "ban on composite", Duration.ZERO);
		Punishment warnOnAddress = addPunishment(WARN, AddressVictim.of(address), "warn on address", Duration.ZERO);
		Punishment expiredMuteOnUuid = addPunishment(MUTE, PlayerVictim.of(uuid), "expired mute on uuid", Duration.ofSeconds(1L));
		time.advanceBy(Duration.ofSeconds(2L));

		assertEquals(
				List.of(banOnUuid, banOnAddress, banOnComposite, warnOnAddress, expiredMuteOnUuid),
				selector.selectionByApplicabilityBuilder(uuid, address)
						.selectAll()
						.build()
						.getAllSpecificPunishments(SortPunishments.OLDEST_FIRST)
						.toCompletableFuture()
						.join()
		);
	}

	@TestTemplate
	@SetAddressStrictness(AddressStrictness.STRICT)
	@IrrelevantData
	public void avoidDuplicateApplicablePunishments() {
		UUID uuid = UUID.randomUUID();
		NetworkAddress address = RandomUtil.randomAddress();
		strictnessAssertHelper.connectAndAssertUnbannedUser(uuid, "username", address);
		UUID altUuid = UUID.randomUUID();
		NetworkAddress altAddress = RandomUtil.randomAddress();
		strictnessAssertHelper.connectAndAssertUnbannedUser(altUuid, "alt username", address);
		strictnessAssertHelper.connectAndAssertUnbannedUser(altUuid, "alt username", altAddress);

		Punishment banOnAddress = addPunishment(BAN, AddressVictim.of(address), "ban on address", Duration.ofMinutes(2L));
		Punishment muteOnAltUuid = addPunishment(MUTE, PlayerVictim.of(altUuid), "mute on alt uuid", Duration.ZERO);
		Punishment warnOnAltAddress = addPunishment(WARN, AddressVictim.of(altAddress), "warn on alt address", Duration.ofMinutes(3L));

		assertEquals(
				List.of(muteOnAltUuid),
				selector.selectionByApplicabilityBuilder(uuid, address)
						.type(MUTE)
						.build()
						.getAllSpecificPunishments()
						.toCompletableFuture()
						.join()
		);
		assertEquals(
				List.of(banOnAddress),
				selector.selectionByApplicabilityBuilder(uuid, address)
						.type(BAN)
						.build()
						.getAllSpecificPunishments()
						.toCompletableFuture()
						.join()
		);
		assertEquals(
				List.of(muteOnAltUuid, warnOnAltAddress, banOnAddress), // Ordered by end date
				selector.selectionByApplicabilityBuilder(uuid, address)
						.build()
						.getAllSpecificPunishments(SortPunishments.LATEST_END_DATE_FIRST)
						.toCompletableFuture()
						.join()
		);
	}

}
