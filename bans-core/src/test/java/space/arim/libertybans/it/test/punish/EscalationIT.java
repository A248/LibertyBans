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

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDetailsCalculator;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.IrrelevantData;
import space.arim.libertybans.it.resolver.NonNullTrack;
import space.arim.libertybans.it.resolver.RandomEscalationTrackResolver;
import space.arim.libertybans.it.resolver.RandomOperatorResolver;
import space.arim.libertybans.it.resolver.RandomVictimResolver;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith({RandomVictimResolver.class, RandomOperatorResolver.class, RandomEscalationTrackResolver.class})
public class EscalationIT {

	private final PunishmentDrafter drafter;
	private final ScopeManager scopeManager;
	private final Victim victim;
	private final EscalationTrack escalationTrack;

	public EscalationIT(PunishmentDrafter drafter, ScopeManager scopeManager, 
						@DontInject Victim victim, @DontInject @NonNullTrack EscalationTrack escalationTrack) {
		this.drafter = drafter;
		this.scopeManager = scopeManager;
		this.victim = victim;
		this.escalationTrack = escalationTrack;
	}

	private void addPunishment(PunishmentType type, Operator operator, String reason) {
		drafter
				.draftBuilder()
				.type(type)
				.victim(victim)
				.operator(operator)
				.reason(reason)
				.escalationTrack(escalationTrack)
				.build()
				.enactPunishment()
				.toCompletableFuture()
				.join()
				.orElseThrow(AssertionError::new);
	}

	private Punishment calculate(Operator operator, PunishmentDetailsCalculator calculator) {
		return drafter
				.calculablePunishmentBuilder()
				.victim(victim)
				.operator(operator)
				.escalationTrack(escalationTrack)
				.calculator(calculator)
				.build()
				.enactPunishment()
				.toCompletableFuture()
				.join()
				.orElseThrow(AssertionError::new);
	}

	@TestTemplate
	public void escalateSimply(@DontInject Operator operator1, @DontInject Operator operator2) {
		PunishmentDetailsCalculator calculator = (track, victim, selectionOrderBuilder) -> {
			assertEquals(escalationTrack, track);
			Integer existingPunishments = selectionOrderBuilder
					.victim(victim)
					.escalationTrack(track)
					.build()
					.countNumberOfPunishments()
					.toCompletableFuture()
					.getNow(null);
			if (existingPunishments == null) {
				throw new AssertionError("Selection should be instantly complete");
			}
			PunishmentType type = PunishmentType.WARN;
			return new PunishmentDetailsCalculator.CalculationResult(
					type, "Now at " + (existingPunishments + 1), Duration.ZERO, scopeManager.globalScope()
			);
		};
		addPunishment(PunishmentType.WARN, operator1, "first warn");
		assertEquals("Now at 2", calculate(operator1, calculator).getReason());
		assertEquals("Now at 3", calculate(operator2, calculator).getReason());
		addPunishment(PunishmentType.MUTE, operator1, "now muted");
		assertEquals("Now at 5", calculate(operator2, calculator).getReason());
		addPunishment(PunishmentType.BAN, operator2, "now banned");
		assertEquals("Now at 7", calculate(operator1, calculator).getReason());
	}

	@TestTemplate
	@IrrelevantData
	public void escalateWithExtraData(@DontInject Operator operator1, @DontInject Operator operator2) {
		PunishmentDetailsCalculator calculator = (track, victim, selectionOrderBuilder) -> {
			Integer existingPunishments = selectionOrderBuilder
					.victim(victim)
					.escalationTrack(track)
					.build()
					.countNumberOfPunishments()
					.toCompletableFuture()
					.getNow(null);
			if (existingPunishments == null) {
				throw new AssertionError("Selection should be instantly complete");
			}
			PunishmentType type = PunishmentType.WARN;
			return new PunishmentDetailsCalculator.CalculationResult(
					type, "Now at " + (existingPunishments + 1), Duration.ZERO, scopeManager.globalScope()
			);
		};
		addPunishment(PunishmentType.WARN, operator1, "first warn");
		assertEquals("Now at 2", calculate(operator1, calculator).getReason());
		assertEquals("Now at 3", calculate(operator2, calculator).getReason());
		addPunishment(PunishmentType.MUTE, operator1, "now muted");
		assertEquals("Now at 5", calculate(operator2, calculator).getReason());
		addPunishment(PunishmentType.BAN, operator2, "now banned");
		assertEquals("Now at 7", calculate(operator1, calculator).getReason());
	}

}
