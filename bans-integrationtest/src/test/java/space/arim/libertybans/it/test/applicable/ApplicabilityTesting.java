/* 
 * LibertyBans-integrationtest
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-integrationtest is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LibertyBans-integrationtest is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-integrationtest. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.it.test.applicable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.DraftPunishment;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.revoke.PunishmentRevoker;
import space.arim.libertybans.api.revoke.RevocationOrder;
import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.it.util.TestingUtil;

public class ApplicabilityTesting extends ApplicabilityTestingBase {
	
	public ApplicabilityTesting(LibertyBansCore core, PunishmentType type, Operator operator) {
		super(core, type, operator);
	}
	
	@Override
	public void doTest() {
		testApplicableUUID();
		testApplicableAddress();
	}
	
	private void testApplicableUUID() {
		UUID uuid = UUID.randomUUID();
		InetAddress address = TestingUtil.randomAddress();
		assertNull(core.getEnforcementCenter().executeAndCheckConnection(uuid, "whoknows", address).join());
		Victim victim = PlayerVictim.of(uuid);
		testApplicable(victim, uuid, NetworkAddress.of(address));
	}
	
	private void testApplicableAddress() {
		UUID uuid = UUID.randomUUID();
		InetAddress address = TestingUtil.randomAddress();
		assertNull(core.getEnforcementCenter().executeAndCheckConnection(uuid, "another", address).join());
		AddressVictim victim = AddressVictim.of(address);
		testApplicable(victim, uuid, victim.getAddress());
	}
	
	private void testApplicable(Victim victim, UUID uuid, NetworkAddress address) {

		String reason = TestingUtil.randomString(TestingUtil.random().nextInt(100));

		DraftPunishment draft = core.getDrafter().draftBuilder().type(type).victim(victim)
				.operator(operator).reason(reason).build();
		Punishment enacted;
		if (TestingUtil.randomBoolean()) {
			enacted = draft.enactPunishment().join();
		} else {
			enacted = draft.enactPunishmentWithoutEnforcement().join();
		}
		assertNotNull(enacted, "Initial enactment failed for " + info);
		TestingUtil.assertEqualDetails(draft, enacted);

		if (type == PunishmentType.KICK) {
			// Kicks are always history, therefore never applicable, and cannot be undone
			return;
		}

		Punishment selected = core.getSelector().getApplicablePunishment(uuid, address, type).join();
		assertNotNull(selected, "Applicability selection failed for " + info);
		TestingUtil.assertEqualDetails(enacted, selected);

		testUndo(enacted);
	}
	
	private void testUndo(Punishment enacted) {
		int randomFrom1To4 = TestingUtil.random().nextInt(4) + 1;
		PunishmentRevoker revoker = core.getRevoker();
		RevocationOrder order;

		switch (randomFrom1To4) {
		case 1:
			if (TestingUtil.randomBoolean()) {
				assertTrue(enacted.undoPunishment().join());
			} else {
				assertTrue(enacted.undoPunishmentWithoutUnenforcement().join());
			}
			return;
		case 2:
			order = revoker.revokeById(enacted.getID());
			break;
		case 3:
			order = revoker.revokeByIdAndType(enacted.getID(), type);
			break;
		case 4:
			if (type.isSingular()) {
				order = revoker.revokeByTypeAndVictim(type, enacted.getVictim());
			} else {
				order = revoker.revokeByIdAndType(enacted.getID(), type);
			}
			break;
		default:
			throw Assertions.<RuntimeException>fail("Testing code is broken");
		}
		if (TestingUtil.randomBoolean()) {
			Punishment retrieved;
			if (TestingUtil.randomBoolean()) {
				retrieved = order.undoAndGetPunishment().join();
			} else {
				retrieved = order.undoAndGetPunishmentWithoutUnenforcement().join();
			}
			assertNotNull(retrieved, "Undoing and retrieving of enacted punishment failed for " + info);
			TestingUtil.assertEqualDetails(enacted, retrieved);

		} else {
			boolean result;
			if (TestingUtil.randomBoolean()) {
				result = order.undoPunishment().join();
			} else {
				result = order.undoPunishmentWithoutUnenforcement().join();
			}
			assertTrue(result, "Undoing of enacted punishment failed for " + info);
		}
	}
	
}
