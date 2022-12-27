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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.DraftPunishmentBuilder;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.api.select.SelectionOrderBuilder;
import space.arim.libertybans.api.select.SelectionPredicate;
import space.arim.libertybans.core.service.SettableTime;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.resolver.NotConsole;
import space.arim.libertybans.it.resolver.RandomOperatorResolver;
import space.arim.libertybans.it.resolver.RandomPunishmentTypeResolver;
import space.arim.libertybans.it.resolver.RandomPunishmentTypeResolver.SingularPunishment;
import space.arim.libertybans.it.resolver.RandomVictimResolver;
import space.arim.libertybans.it.util.RandomUtil;
import space.arim.omnibus.util.concurrent.CentralisedFuture;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith({RandomPunishmentTypeResolver.class, RandomOperatorResolver.class, RandomVictimResolver.class})
public class SelectionIT {

	private final PunishmentDrafter drafter;
	private final PunishmentSelector selector;
	private final ScopeManager scopeManager;
	private final SettableTime time;

	private static final Duration ONE_SECOND = Duration.ofSeconds(1L);
	private static final Duration TWO_SECONDS = ONE_SECOND.multipliedBy(2L);

	public SelectionIT(PunishmentDrafter drafter, PunishmentSelector selector,
					   ScopeManager scopeManager, SettableTime time) {
		this.drafter = drafter;
		this.selector = selector;
		this.scopeManager = scopeManager;
		this.time = time;
	}

	private SelectionOrderBuilder selectionBuilder(PunishmentType type) {
		return selector.selectionBuilder().type(type);
	}

	private static Punishment getSinglePunishment(SelectionOrderBuilder selectionBuilder) {
		return selectionBuilder.build().getFirstSpecificPunishment().toCompletableFuture().join().orElseThrow();
	}

	private static List<Punishment> getPunishments(SelectionOrderBuilder selectionBuilder) {
		return selectionBuilder.build().getAllSpecificPunishments().toCompletableFuture().join();
	}

	private static void assertEmpty(SelectionOrderBuilder selectionBuilder) {
		List<Punishment> punishments = getPunishments(selectionBuilder);
		assertTrue(punishments.isEmpty(), "Non-empty punishments, retrieved " + punishments);
	}

	@TestTemplate
	public void selectNothing(@DontInject PunishmentType type, @DontInject Victim victim, @DontInject Operator operator) {
		assertEmpty(selector.selectionBuilder());
		assertEmpty(selector.selectionBuilder().operator(operator));
		assertEmpty(selector.selectionBuilder().victim(victim));
		assertEmpty(selectionBuilder(type));
		assertEmpty(selectionBuilder(type).operator(operator));
		assertEmpty(selectionBuilder(type).victim(victim));
		assertEmpty(selectionBuilder(type).operator(operator).victim(victim));
		assertEmpty(selectionBuilder(type).scope(scopeManager.specificScope("servername")));
		assertEmpty(selectionBuilder(type).victim(victim).scope(scopeManager.specificScope("servername")));
		assertEmpty(selectionBuilder(type).limitToRetrieve(20));
		assertEmpty(selectionBuilder(type).operator(operator).limitToRetrieve(15));
		assertEmpty(selectionBuilder(type).victim(victim).skipFirstRetrieved(10).limitToRetrieve(30));
		assertEmpty(selectionBuilder(type).selectAll().victim(victim).operator(operator));
		assertEmpty(selectionBuilder(type).selectAll().scope(scopeManager.specificScope("otherserver")));
	}

	private DraftPunishmentBuilder draftBuilder(PunishmentType type, Victim victim, String reason) {
		return drafter.draftBuilder().type(type).victim(victim).reason(reason);
	}

	private Punishment getPunishment(DraftPunishmentBuilder draftBuilder) {
		CentralisedFuture<Optional<Punishment>> future = draftBuilder.build().enactPunishment().toCompletableFuture();
		Optional<Punishment> optPunishment;
		try {
			optPunishment = future.join();
		} catch (CompletionException ex) {
			throw Assertions.<RuntimeException>fail("Drafting punishment to later select failed", ex);
			//throw new TestAbortedException("Drafting punishment to later select failed", ex);
		}
		assertTrue(optPunishment.isPresent(), "Drafting punishment to later select failed");
		return optPunishment.get();
	}

	@TestTemplate
	public void selectBanMuteForVictim(@DontInject @SingularPunishment PunishmentType type, @DontInject Victim victim) {
		Punishment banOrMute = getPunishment(
				draftBuilder(type, victim, "some reason").scope(scopeManager.specificScope("someserver")));
		assertEquals(banOrMute, getSinglePunishment(selectionBuilder(type).victim(victim)));
	}

	@TestTemplate
	public void selectMultipleWarnsForVictim(@DontInject Victim victim) {
		// Kicks would not be considered active, whereas bans and mutes are singular
		final PunishmentType type = PunishmentType.WARN;

		Punishment pun1 = getPunishment(
				draftBuilder(type, victim, "and kicked/warned on top of that"));
		// Required to have punishment start times be correctly ordered
		time.advanceBy(ONE_SECOND);

		Punishment pun2 = getPunishment(
				draftBuilder(type, victim, "Yet another punishment"));
		time.advanceBy(ONE_SECOND);

		Punishment pun3 = getPunishment(
				draftBuilder(type, victim, "More punishments all around"));

		assertEquals(
				List.of(pun3, pun2, pun1),
				getPunishments(selectionBuilder(type).victim(victim)));
		assertEquals(
				List.of(pun2, pun1),
				getPunishments(selectionBuilder(type).victim(victim).skipFirstRetrieved(1)));
		assertEquals(
				List.of(pun3),
				getPunishments(selectionBuilder(type).victim(victim).limitToRetrieve(1)));
		assertEquals(
				List.of(pun2),
				getPunishments(selectionBuilder(type).victim(victim).skipFirstRetrieved(1).limitToRetrieve(1))
		);
	}

	@TestTemplate
	public void selectHistoricalBansMutes(@DontInject @SingularPunishment PunishmentType type,
										  @DontInject Victim victim) {
		Punishment expired1 = getPunishment(
				draftBuilder(type, victim, "the first punishment").duration(ONE_SECOND));
		time.advanceBy(TWO_SECONDS);

		Punishment expired2 = getPunishment(
				draftBuilder(type, victim, "the second punishment").duration(ONE_SECOND));
		time.advanceBy(TWO_SECONDS);

		Punishment active = getPunishment(
				draftBuilder(type, victim, "the third punishment"));

		assertEquals(
				List.of(active),
				getPunishments(selector.selectionBuilder().victim(victim)));
		assertEquals(
				List.of(active),
				getPunishments(selectionBuilder(type).victim(victim)));
		assertEquals(
				List.of(active, expired2, expired1),
				getPunishments(selectionBuilder(type).victim(victim).selectAll()));
		assertEquals(
				List.of(expired1),
				getPunishments(selectionBuilder(type).victim(victim).selectAll().skipFirstRetrieved(2)));
		assertEquals(
				List.of(active, expired2),
				getPunishments(selectionBuilder(type).victim(victim).selectAll().limitToRetrieve(2)));
	}

	@TestTemplate
	public void selectBlame(@DontInject Victim victim1, @DontInject Victim victim2,
							@DontInject Operator operator1, @DontInject @NotConsole Operator operator2) {
		Punishment banFromOp1 = getPunishment(
				draftBuilder(PunishmentType.BAN, victim1, "banhammer").operator(operator1));
		Punishment warnFromOp2 = getPunishment(
				draftBuilder(PunishmentType.WARN, victim1, "warning").operator(operator2));
		time.advanceBy(ONE_SECOND);

		Punishment muteOfVictim2FromOp1 = getPunishment(
				draftBuilder(PunishmentType.MUTE, victim2, "muted").operator(operator1));

		assertEquals(
				List.of(muteOfVictim2FromOp1, banFromOp1),
				getPunishments(selector.selectionBuilder().operator(operator1)));
		assertEquals(
				List.of(warnFromOp2),
				getPunishments(selector.selectionBuilder().operator(operator2)));
	}

	@TestTemplate
	public void selectVictimsBySelfOrType() {
		PlayerVictim uuid1 = PlayerVictim.of(UUID.randomUUID());
		PlayerVictim uuid2 = PlayerVictim.of(UUID.randomUUID());
		AddressVictim address1 = AddressVictim.of(RandomUtil.randomAddress());
		AddressVictim address2 = AddressVictim.of(RandomUtil.randomAddress());

		Punishment banUuid1 = getPunishment(
				draftBuilder(PunishmentType.BAN, uuid1, "banned"));
		Punishment warnUuid2 = getPunishment(
				draftBuilder(PunishmentType.WARN, uuid2, "warned"));
		Punishment banAddress1 = getPunishment(
				draftBuilder(PunishmentType.BAN, address1, "kicked"));
		Punishment muteAddress2 = getPunishment(
				draftBuilder(PunishmentType.MUTE, address2, "muted"));

		assertEquals(
				List.of(warnUuid2, banUuid1),
				getPunishments(selector.selectionBuilder().victimType(Victim.VictimType.PLAYER)));
		assertEquals(
				List.of(muteAddress2, banAddress1),
				getPunishments(selector.selectionBuilder().victimTypes(
						SelectionPredicate.matchingAnyOf(Victim.VictimType.ADDRESS, Victim.VictimType.COMPOSITE)
				)));
		assertEquals(
				List.of(muteAddress2, banAddress1),
				getPunishments(selector.selectionBuilder().victimTypes(
						SelectionPredicate.matchingNone(Victim.VictimType.PLAYER)
				)),
				"Should be identical to previous assertion");
		assertEquals(
				List.of(),
				getPunishments(selector.selectionBuilder().victimType(Victim.VictimType.COMPOSITE)));
		assertEquals(
				List.of(warnUuid2),
				getPunishments(selector.selectionBuilder().victim(uuid2)));
		assertEquals(
				List.of(banAddress1, warnUuid2),
				getPunishments(selector.selectionBuilder().victims(SelectionPredicate.matchingAnyOf(uuid2, address1))));
		assertEquals(
				List.of(banAddress1, warnUuid2),
				getPunishments(selector.selectionBuilder().victims(SelectionPredicate.matchingNone(uuid1, address2))),
				"Should be identical to previous assertion (in context)");
	}

	@TestTemplate
	public void selectWarnCount() {
		Victim victim = PlayerVictim.of(UUID.randomUUID());

		getPunishment(
				draftBuilder(PunishmentType.WARN, victim, "warning"));
		getPunishment(
				draftBuilder(PunishmentType.WARN, victim, "another warning"));
		int warnCount = selector.selectionBuilder()
				.victim(victim)
				.seekBefore(Instant.now(), Long.MAX_VALUE)
				.selectActiveOnly()
				.build()
				.countNumberOfPunishments()
				.toCompletableFuture()
				.join();
		assertEquals(2, warnCount);
	}

}
