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
package space.arim.libertybans.it.test.punish;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.revoke.PunishmentRevoker;
import space.arim.libertybans.api.revoke.RevocationOrder;
import space.arim.libertybans.core.punish.Enforcer;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetAddressStrictness;
import space.arim.libertybans.it.resolver.RandomOperatorResolver;
import space.arim.libertybans.it.resolver.RandomPunishmentTypeResolver;
import space.arim.libertybans.it.resolver.RandomReasonResolver;
import space.arim.libertybans.it.resolver.RandomReasonResolver.Reason;
import space.arim.libertybans.it.resolver.RandomVictimResolver;
import space.arim.libertybans.it.util.RandomUtil;
import space.arim.libertybans.it.util.TestingUtil;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith({RandomPunishmentTypeResolver.class, RandomOperatorResolver.class,
	RandomVictimResolver.class, RandomReasonResolver.class})
public class PunishUnpunishIT {

	private final PunishmentDrafter drafter;
	private final PunishmentRevoker revoker;
	private final Enforcer enforcer;
	private final PunishmentType type;
	private final Operator operator;
	private final Victim victim;
	private final String reason;

	@Inject
	public PunishUnpunishIT(PunishmentDrafter drafter, PunishmentRevoker revoker, Enforcer enforcer,
			@DontInject PunishmentType type, @DontInject Operator operator, @DontInject Victim victim,
			@DontInject @Reason String reason) {
		this.drafter = drafter;
		this.revoker = revoker;
		this.enforcer = enforcer;
		this.type = type;
		this.operator = operator;
		this.victim = victim;
		this.reason = reason;
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void testPunishThenUndo() {
		DraftPunishment draft = drafter.draftBuilder().type(type).victim(victim)
				.operator(operator).reason(reason).build();

		ReactionStage<Optional<Punishment>> enactedFuture;
		if (RandomUtil.randomBoolean()) {
			enactedFuture = draft.enactPunishment();
		} else {
			enactedFuture = draft.enactPunishmentWithoutEnforcement();
		}
		Punishment enacted = enactedFuture.toCompletableFuture().join().orElse(null);

		assertNotNull(enacted, "Initial enactment failed for " + this);
		TestingUtil.assertEqualDetails(draft, enacted);

		if (type == PunishmentType.KICK) {
			// Kicks are always history, therefore never applicable, and cannot be undone
			return;
		}

		testUndo(enacted);
	}

	private void testUndo(Punishment enacted) {
		int randomFrom1To4 = ThreadLocalRandom.current().nextInt(4) + 1;
		RevocationOrder order;

		switch (randomFrom1To4) {

		case 1: /* Revoke directly */
			ReactionStage<Boolean> undoFuture;
			if (RandomUtil.randomBoolean()) {
				undoFuture = enacted.undoPunishment();
			} else {
				undoFuture = enacted.undoPunishmentWithoutUnenforcement();
			}
			assertTrue(undoFuture.toCompletableFuture().join());
			return;

		case 2: /* Revoke by ID */
			order = revoker.revokeById(enacted.getID());
			break;

		case 3: /* Revoke by ID and Type */
			order = revoker.revokeByIdAndType(enacted.getID(), type);
			break;

		case 4: /* Revoke by Type and Victim if possible */
			if (type.isSingular()) {
				order = revoker.revokeByTypeAndVictim(type, enacted.getVictim());
			} else {
				order = revoker.revokeByIdAndType(enacted.getID(), type);
			}
			break;
		default:
			throw Assertions.<RuntimeException>fail("Integration test is broken");
		}
		if (RandomUtil.randomBoolean()) { /* Undo and get*/

			ReactionStage<Optional<Punishment>> retrievedFuture;
			if (RandomUtil.randomBoolean()) {
				retrievedFuture = order.undoAndGetPunishment();
			} else {
				retrievedFuture = order.undoAndGetPunishmentWithoutUnenforcement();
			}
			Punishment retrieved = retrievedFuture.toCompletableFuture().join().orElse(null);

			assertNotNull(retrieved, "Undoing and retrieving of enacted punishment failed for " + this);
			TestingUtil.assertEqualDetails(enacted, retrieved);

		} else { /* Simple undo */

			ReactionStage<Boolean> result;
			if (RandomUtil.randomBoolean()) {
				result = order.undoPunishment();
			} else {
				result = order.undoPunishmentWithoutUnenforcement();
			}
			assertTrue(result.toCompletableFuture().join(), "Undoing of enacted punishment failed for " + this);
		}
	}

	@Override
	public String toString() {
		return "PunishIT [drafter=" + drafter + ", revoker=" + revoker + ", enforcer=" + enforcer + ", type=" + type
				+ ", operator=" + operator + ", victim=" + victim + ", reason=" + reason + "]";
	}

}
