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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static space.arim.libertybans.it.util.TestingUtil.assertEqualDetails;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith({RandomPunishmentTypeResolver.class, RandomOperatorResolver.class,
	RandomVictimResolver.class, RandomReasonResolver.class})
@RandomPunishmentTypeResolver.NotAKick
public class PunishUnpunishIT {

	private final PunishmentDrafter drafter;
	private final PunishmentRevoker revoker;
	private final Enforcer enforcer;
	private PunishmentType type;
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

	private DraftPunishment draftPunishment() {
		return drafter.draftBuilder().type(type).victim(victim)
				.operator(operator).reason(reason).build();
	}

	private void ensureTypeSingular() {
		while (!type.isSingular()) {
			type = RandomPunishmentTypeResolver.randomPunishmentType();
		}
	}

	// Simple enact and revoke

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void simpleEnactAndUndo() {
		var draft = draftPunishment();
		assertTrue(draft.enactPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new)
				.undoPunishment()
				.toCompletableFuture()
				.join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void simpleEnactWithoutEnforcementAndUndo() {
		var draft = draftPunishment();
		assertTrue(draft.enactPunishmentWithoutEnforcement().toCompletableFuture().join()
				.orElseThrow(AssertionError::new)
				.undoPunishment()
				.toCompletableFuture()
				.join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void simpleEnactAndUndoWithoutUnenforcement() {
		var draft = draftPunishment();
		assertTrue(draft.enactPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new)
				.undoPunishmentWithoutUnenforcement()
				.toCompletableFuture()
				.join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void simpleEnactWithoutEnforcementAndUndoWithoutUnenforcement() {
		var draft = draftPunishment();
		assertTrue(draft.enactPunishmentWithoutEnforcement().toCompletableFuture().join()
				.orElseThrow(AssertionError::new)
				.undoPunishmentWithoutUnenforcement()
				.toCompletableFuture()
				.join());
	}

	// Revoke by ID

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeById() {
		var draft = draftPunishment();
		Punishment punishment = draft.enactPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeById(punishment.getIdentifier());
		assertTrue(revocationOrder.undoPunishment().toCompletableFuture().join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeByIdWithoutUnenforcement() {
		var draft = draftPunishment();
		Punishment punishment = draft.enactPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeById(punishment.getIdentifier());
		assertTrue(revocationOrder.undoPunishmentWithoutUnenforcement().toCompletableFuture().join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeAndGetById() {
		var draft = draftPunishment();
		Punishment punishment = draft.enactPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeById(punishment.getIdentifier());
		Punishment revoked = revocationOrder.undoAndGetPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		assertEqualDetails(punishment, revoked);
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeandGetByIdWithoutUnenforcement() {
		var draft = draftPunishment();
		Punishment punishment = draft.enactPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeById(punishment.getIdentifier());
		Punishment revoked = revocationOrder.undoAndGetPunishmentWithoutUnenforcement().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		assertEqualDetails(punishment, revoked);
	}

	// Revoke by ID and type

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeByIdAndType() {
		var draft = draftPunishment();
		Punishment punishment = draft.enactPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByIdAndType(punishment.getIdentifier(), punishment.getType());
		assertTrue(revocationOrder.undoPunishment().toCompletableFuture().join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeByIdAndTypeWithoutUnenforcement() {
		var draft = draftPunishment();
		Punishment punishment = draft.enactPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByIdAndType(punishment.getIdentifier(), punishment.getType());
		assertTrue(revocationOrder.undoPunishmentWithoutUnenforcement().toCompletableFuture().join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeAndGetByIdAndType() {
		var draft = draftPunishment();
		Punishment punishment = draft.enactPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByIdAndType(punishment.getIdentifier(), punishment.getType());
		Punishment revoked = revocationOrder.undoAndGetPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		assertEqualDetails(punishment, revoked);
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeAndGetByIdAndTypeWithoutUnenforcement() {
		var draft = draftPunishment();
		Punishment punishment = draft.enactPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByIdAndType(punishment.getIdentifier(), punishment.getType());
		Punishment revoked = revocationOrder.undoAndGetPunishmentWithoutUnenforcement().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		assertEqualDetails(punishment, revoked);
	}

	// Revoke by type and victim

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeByTypeAndVictim() {
		ensureTypeSingular();
		var draft = draftPunishment();
		Punishment punishment = draft.enactPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByTypeAndVictim(punishment.getType(), punishment.getVictim());
		assertTrue(revocationOrder.undoPunishment().toCompletableFuture().join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeByTypeAndVictimWithoutUnenforcement() {
		ensureTypeSingular();
		var draft = draftPunishment();
		Punishment punishment = draft.enactPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByTypeAndVictim(punishment.getType(), punishment.getVictim());
		assertTrue(revocationOrder.undoPunishmentWithoutUnenforcement().toCompletableFuture().join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeAndGetByTypeAndVictim() {
		ensureTypeSingular();
		var draft = draftPunishment();
		Punishment punishment = draft.enactPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByTypeAndVictim(punishment.getType(), punishment.getVictim());
		Punishment revoked = revocationOrder.undoAndGetPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		assertEqualDetails(punishment, revoked);
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeAndGetByTypeAndVictimWithoutUnenforcement() {
		ensureTypeSingular();
		var draft = draftPunishment();
		Punishment punishment = draft.enactPunishment().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByTypeAndVictim(punishment.getType(), punishment.getVictim());
		Punishment revoked = revocationOrder.undoAndGetPunishmentWithoutUnenforcement().toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		assertEqualDetails(punishment, revoked);
	}

	@Override
	public String toString() {
		return "PunishIT [drafter=" + drafter + ", revoker=" + revoker + ", enforcer=" + enforcer + ", type=" + type
				+ ", operator=" + operator + ", victim=" + victim + ", reason=" + reason + "]";
	}

}
