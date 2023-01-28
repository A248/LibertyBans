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

package space.arim.libertybans.it.test.select;

import jakarta.inject.Inject;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.resolver.RandomOperatorResolver;
import space.arim.libertybans.it.resolver.RandomReasonResolver;
import space.arim.libertybans.it.resolver.RandomReasonResolver.Reason;
import space.arim.libertybans.it.resolver.RandomVictimResolver;
import space.arim.libertybans.it.test.applicable.StrictnessAssertHelper;
import space.arim.libertybans.it.util.RandomUtil;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith({RandomOperatorResolver.class, RandomVictimResolver.class, RandomReasonResolver.class})
public class SelectorIT {

	private final StrictnessAssertHelper assertHelper;
	private final PunishmentDrafter drafter;
	private final PunishmentSelector selector;
	private final Operator operator;
	private final Victim victim;
	private final String reason;

	@Inject
	public SelectorIT(StrictnessAssertHelper assertHelper, PunishmentDrafter drafter, PunishmentSelector selector,
					  @DontInject Operator operator, @DontInject Victim victim, @DontInject @Reason String reason) {
		this.assertHelper = assertHelper;
		this.drafter = drafter;
		this.selector = selector;
		this.operator = operator;
		this.victim = victim;
		this.reason = reason;
	}

	private Punishment enactPunishment(PunishmentType type, Victim victim) {
		return drafter.draftBuilder()
				.type(type)
				.victim(victim)
				.operator(operator)
				.reason(reason)
				.build()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
	}

	private Punishment enactPunishment(PunishmentType type) {
		return enactPunishment(type, victim);
	}

	@TestTemplate
	public void selectActiveById() {
		Punishment punishment = enactPunishment(PunishmentType.WARN);
		long id = punishment.getIdentifier();
		assertEquals(
				punishment,
				selector.getActivePunishmentById(id).toCompletableFuture().join().orElse(null)
		);
		assertEquals(
				punishment,
				selector.getActivePunishmentByIdAndType(id, PunishmentType.WARN).toCompletableFuture().join().orElse(null)
		);
		assertNull(
				selector.getActivePunishmentByIdAndType(id, PunishmentType.MUTE).toCompletableFuture().join().orElse(null),
				"Different type requested"
		);
		// Undo punishment and check historical
		assumeTrue(punishment.undoPunishment().toCompletableFuture().join());
		assertNull(
				selector.getActivePunishmentById(id).toCompletableFuture().join().orElse(null),
				"Punishment no longer active"
		);
		assertNull(
				selector.getActivePunishmentByIdAndType(id, PunishmentType.WARN).toCompletableFuture().join().orElse(null),
				"Punishment no longer active"
		);
	}

	@TestTemplate
	public void selectHistoricalById() {
		Punishment punishment = enactPunishment(PunishmentType.MUTE);
		long id = punishment.getIdentifier();
		assertEquals(
				punishment,
				selector.getHistoricalPunishmentById(id).toCompletableFuture().join().orElse(null)
		);
		assertEquals(
				punishment,
				selector.getHistoricalPunishmentByIdAndType(id, PunishmentType.MUTE).toCompletableFuture().join().orElse(null)
		);
		assertNull(
				selector.getHistoricalPunishmentByIdAndType(id, PunishmentType.WARN).toCompletableFuture().join().orElse(null),
				"Different type requested"
		);
	}

	@TestTemplate
	public void selectMute() {
		UUID uuid = UUID.randomUUID();
		NetworkAddress address = RandomUtil.randomAddress();
		assertHelper.connectAndAssumeUnbannedUser(uuid, "name", address);

		Punishment punishment = enactPunishment(PunishmentType.MUTE, PlayerVictim.of(uuid));
		assertEquals(
				punishment,
				selector.getApplicablePunishment(uuid, address, PunishmentType.MUTE).toCompletableFuture().join().orElse(null)
		);
	}

}
