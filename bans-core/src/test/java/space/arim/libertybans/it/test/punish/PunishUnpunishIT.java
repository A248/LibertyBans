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
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.EscalationTrack;
import space.arim.libertybans.api.punish.ExpunctionOrder;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.punish.PunishmentRevoker;
import space.arim.libertybans.api.punish.RevocationOrder;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.punish.EnforcementOpts;
import space.arim.libertybans.core.punish.LocalEnforcer;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetAddressStrictness;
import space.arim.libertybans.it.resolver.RandomEscalationTrackResolver;
import space.arim.libertybans.it.resolver.RandomOperatorResolver;
import space.arim.libertybans.it.resolver.RandomPunishmentTypeResolver;
import space.arim.libertybans.it.resolver.RandomReasonResolver;
import space.arim.libertybans.it.resolver.RandomReasonResolver.Reason;
import space.arim.libertybans.it.resolver.RandomVictimResolver;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static space.arim.libertybans.it.util.TestingUtil.assertEqualDetails;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith({
		RandomPunishmentTypeResolver.class, RandomVictimResolver.class, RandomOperatorResolver.class,
		RandomReasonResolver.class, RandomEscalationTrackResolver.class})
@RandomPunishmentTypeResolver.NotAKick
public class PunishUnpunishIT {

	private final PunishmentDrafter drafter;
	private final PunishmentRevoker revoker;
	private final LocalEnforcer enforcer;
	private PunishmentType type;
	private final Victim victim;
	private final Operator operator;
	private final String reason;
	private final EscalationTrack escalationTrack;

	@Inject
	public PunishUnpunishIT(PunishmentDrafter drafter, PunishmentRevoker revoker, LocalEnforcer enforcer,
							@DontInject PunishmentType type, @DontInject Victim victim, @DontInject Operator operator,
							@DontInject @Reason String reason, @DontInject EscalationTrack escalationTrack) {
		this.drafter = drafter;
		this.revoker = revoker;
		this.enforcer = enforcer;
		this.type = type;
		this.victim = victim;
		this.operator = operator;
		this.reason = reason;
		this.escalationTrack = escalationTrack;
	}

	private DraftPunishment draftPunishment() {
		return drafter.draftBuilder()
				.type(type)
				.victim(victim)
				.operator(operator)
				.reason(reason)
				.escalationTrack(escalationTrack)
				.build();
	}

	private void ensureTypeSingular() {
		while (!type.isSingular()) {
			type = RandomPunishmentTypeResolver.randomPunishmentType();
		}
	}

	private static EnforcementOptions noEnforcement() {
		return EnforcementOpts.builder().enforcement(EnforcementOptions.Enforcement.NONE).build();
	}

	// Simple enact and revoke

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void simpleEnactAndUndo() {
		assertTrue(draftPunishment()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new)
				.undoPunishment()
				.toCompletableFuture()
				.join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void simpleEnactWithoutEnforcementAndUndo() {
		assertTrue(draftPunishment()
				.enactPunishment(noEnforcement())
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new)
				.undoPunishment()
				.toCompletableFuture()
				.join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void simpleEnactAndUndoWithoutUnenforcement() {
		assertTrue(draftPunishment()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new)
				.undoPunishment(noEnforcement())
				.toCompletableFuture()
				.join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void simpleEnactWithoutEnforcementAndUndoWithoutUnenforcement() {
		assertTrue(draftPunishment()
				.enactPunishment(noEnforcement())
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new)
				.undoPunishment(noEnforcement())
				.toCompletableFuture()
				.join());
	}

	// Revoke by ID

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeById() {
		Punishment punishment = draftPunishment()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeById(punishment.getIdentifier());
		assertTrue(revocationOrder.undoPunishment().toCompletableFuture().join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeByIdWithoutUnenforcement() {
		Punishment punishment = draftPunishment()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeById(punishment.getIdentifier());
		assertTrue(revocationOrder.undoPunishment(noEnforcement()).toCompletableFuture().join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeAndGetById() {
		Punishment punishment = draftPunishment()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeById(punishment.getIdentifier());
		Punishment revoked = revocationOrder
				.undoAndGetPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		assertEqualDetails(punishment, revoked);
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeandGetByIdWithoutUnenforcement() {
		Punishment punishment = draftPunishment()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeById(punishment.getIdentifier());
		Punishment revoked = revocationOrder
				.undoAndGetPunishment(noEnforcement())
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		assertEqualDetails(punishment, revoked);
	}

	// Revoke by ID and type

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeByIdAndType() {
		Punishment punishment = draftPunishment()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByIdAndType(punishment.getIdentifier(), punishment.getType());
		assertTrue(revocationOrder.undoPunishment().toCompletableFuture().join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeByIdAndTypeWithoutUnenforcement() {
		Punishment punishment = draftPunishment()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByIdAndType(punishment.getIdentifier(), punishment.getType());
		assertTrue(revocationOrder
				.undoPunishment(noEnforcement()
				).toCompletableFuture().join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeAndGetByIdAndType() {
		Punishment punishment = draftPunishment()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByIdAndType(punishment.getIdentifier(), punishment.getType());
		Punishment revoked = revocationOrder
				.undoAndGetPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		assertEqualDetails(punishment, revoked);
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeAndGetByIdAndTypeWithoutUnenforcement() {
		Punishment punishment = draftPunishment()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByIdAndType(punishment.getIdentifier(), punishment.getType());
		Punishment revoked = revocationOrder
				.undoAndGetPunishment(noEnforcement())
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		assertEqualDetails(punishment, revoked);
	}

	// Revoke by type and victim

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeByTypeAndVictim() {
		ensureTypeSingular();
		Punishment punishment = draftPunishment()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByTypeAndVictim(punishment.getType(), punishment.getVictim());
		assertTrue(revocationOrder.undoPunishment().toCompletableFuture().join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeByTypeAndVictimWithoutUnenforcement() {
		ensureTypeSingular();
		Punishment punishment = draftPunishment()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByTypeAndVictim(punishment.getType(), punishment.getVictim());
		assertTrue(revocationOrder.undoPunishment(noEnforcement()).toCompletableFuture().join());
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeAndGetByTypeAndVictim() {
		ensureTypeSingular();
		Punishment punishment = draftPunishment()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByTypeAndVictim(punishment.getType(), punishment.getVictim());
		Punishment revoked = revocationOrder
				.undoAndGetPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		assertEqualDetails(punishment, revoked);
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void revokeAndGetByTypeAndVictimWithoutUnenforcement() {
		ensureTypeSingular();
		Punishment punishment = draftPunishment()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		RevocationOrder revocationOrder = revoker.revokeByTypeAndVictim(punishment.getType(), punishment.getVictim());
		Punishment revoked = revocationOrder
				.undoAndGetPunishment(noEnforcement())
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new);
		assertEqualDetails(punishment, revoked);
	}

	// Expunge

	@TestTemplate
	@SetAddressStrictness(all = true)
	@Inject
	public void expunge(PunishmentSelector selector) {
		long id = draftPunishment()
				.enactPunishment()
				.toCompletableFuture().join()
				.orElseThrow(AssertionError::new)
				.getIdentifier();
		ExpunctionOrder expunctionOrder = revoker.expungePunishment(id);
		assertTrue(expunctionOrder.expunge().toCompletableFuture().join());
		assertEquals(Optional.empty(), selector.getHistoricalPunishmentById(id).toCompletableFuture().join());
	}

	@Override
	public String toString() {
		return "PunishIT [drafter=" + drafter + ", revoker=" + revoker + ", enforcer=" + enforcer + ", type=" + type
				+ ", operator=" + operator + ", victim=" + victim + ", reason=" + reason + "]";
	}

}
