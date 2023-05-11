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

package space.arim.libertybans.it.test.punish;

import jakarta.inject.Inject;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.punish.PunishmentRevoker;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.service.SettableTime;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.env.platform.QuackPlatform;
import space.arim.libertybans.it.env.platform.QuackPlayer;
import space.arim.libertybans.it.env.platform.QuackPlayerBuilder;
import space.arim.libertybans.it.resolver.RandomEscalationTrackResolver;
import space.arim.libertybans.it.resolver.RandomOperatorResolver;
import space.arim.libertybans.it.resolver.RandomPunishmentTypeResolver;
import space.arim.libertybans.it.resolver.RandomVictimResolver;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static space.arim.libertybans.it.util.TestingUtil.assertEqualDetails;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith({RandomPunishmentTypeResolver.class, RandomVictimResolver.class, RandomOperatorResolver.class, RandomEscalationTrackResolver.class})
public class UpdateDetailsIT {

	private final PunishmentDrafter drafter;
	private final PunishmentSelector selector;
	private final PunishmentType type;
	private final Victim victim;
	private final Operator operator;
	private final EscalationTrack escalationTrack;
	private final SettableTime time;

	@Inject
	public UpdateDetailsIT(PunishmentDrafter drafter, PunishmentSelector selector,
						   @DontInject PunishmentType type, @DontInject Victim victim, @DontInject Operator operator,
						   @DontInject EscalationTrack escalationTrack, SettableTime time) {
		this.drafter = drafter;
		this.selector = selector;
		this.type = type;
		this.victim = victim;
		this.operator = operator;
		this.escalationTrack = escalationTrack;
		this.time = time;
	}

	private Punishment createInitial(PunishmentType type, Victim victim, String reason, Duration duration) {
		return drafter.draftBuilder()
				.type(type)
				.victim(victim)
				.operator(operator)
				.reason(reason)
				.escalationTrack(escalationTrack)
				.duration(duration)
				.build()
				.enactPunishment()
				.toCompletableFuture()
				.join()
				.orElseThrow(AssertionError::new);
	}

	private Punishment createInitial(String reason) {
		return createInitial(type, victim, reason, Duration.ZERO);
	}

	private void assertModifications(ReactionStage<Optional<Punishment>> modificationResult,
									 Consumer<Punishment> assertOnNewPunishment) {

		Punishment newPunishment = assertDoesNotThrow(
				modificationResult.toCompletableFuture()::join
		).orElseThrow(AssertionError::new);

		assertOnNewPunishment.accept(newPunishment);
		assertEqualDetails(
				newPunishment,
				selector.getHistoricalPunishmentById(newPunishment.getIdentifier())
						.toCompletableFuture()
						.join()
						.orElseThrow(AssertionError::new)
		);
	}

	@TestTemplate
	public void changeNothing() {
		Punishment original = createInitial("unchanged");
		Punishment newInstance = original.modifyPunishment((editor) -> {})
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		assertSame(original, newInstance);
	}

	@TestTemplate
	public void updateReason() {
		Punishment original = createInitial("first reason");
		assertModifications(
				original.modifyPunishment((editor) -> editor.setReason("new reason")),
				(newPunishment) -> {
					assertEquals("new reason", newPunishment.getReason());
				}
		);
	}

	@TestTemplate
	public void updateEndDateStillTemporary() {
		Punishment original = createInitial(
				PunishmentType.BAN,
				PlayerVictim.of(UUID.randomUUID()),
				"expires soon",
				Duration.ofHours(2L)
		);
		Instant newEndDate = time.currentTimestamp().plus(Duration.ofDays(1L));
		assertModifications(
				original.modifyPunishment((editor) -> {
					editor.setEndDate(newEndDate);
				}),
				(newPunishment) -> {
					assertEquals(newEndDate, newPunishment.getEndDate());
				}
		);
	}

	@TestTemplate
	public void updateEndDateSetPermanent() {
		Punishment original = createInitial(
				PunishmentType.BAN,
				PlayerVictim.of(UUID.randomUUID()),
				"temporary",
				Duration.ofHours(2L)
		);
		assertModifications(
				original.modifyPunishment((editor) -> {
					editor.setEndDate(Punishment.PERMANENT_END_DATE);
				}),
				(newPunishment) -> {
					assertTrue(newPunishment.isPermanent());
				}
		);
	}

	@TestTemplate
	public void updateEndDateExtendActivity() {
		Punishment original = createInitial(
				PunishmentType.BAN,
				PlayerVictim.of(UUID.randomUUID()),
				"expires soon",
				Duration.ofHours(2L)
		);
		assertModifications(
				original.modifyPunishment((editor) -> {
					editor.extendEndDate(Duration.ofHours(3L));
				}),
				(newPunishment) -> {
					assertEquals(original.getEndDate().plus(Duration.ofHours(3L)), newPunishment.getEndDate());
				}
		);
	}

	@TestTemplate
	@Inject
	public void updateEndDateReactivate(QuackPlatform platform) {
		QuackPlayer player = new QuackPlayerBuilder(platform).buildFullyRandom();
		Punishment original = createInitial(
				PunishmentType.BAN,
				PlayerVictim.of(player.getUniqueId()),
				"will expire, but then become re-active",
				Duration.ofHours(1L)
		);
		time.advanceBy(Duration.ofHours(2L));
		// The punishment is now expired, and the banned player may re-join
		player.readdToPlatform();
		// Now, extend the ban and therefore re-activate it
		original.modifyPunishment((editor) -> {
			editor.extendEndDate((Duration.ofHours(3L)));
		}).toCompletableFuture().join();
		// By now the punishment should be re-enforced
		assertFalse(player.isStillOnline());
	}

	@TestTemplate
	@Inject
	public void expungeBeforeUpdate(PunishmentRevoker revoker) {
		Punishment original = createInitial("will be expunged");
		revoker.expungePunishment(original.getIdentifier())
				.expunge()
				.toCompletableFuture().join();
		assertEquals(
				Optional.empty(),
				original.modifyPunishment((editor) -> {
					editor.setReason("some new reason");
				}).toCompletableFuture().join()
		);
	}

}
