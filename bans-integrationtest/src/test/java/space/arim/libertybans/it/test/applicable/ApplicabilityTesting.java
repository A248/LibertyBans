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

import java.util.Random;
import java.util.UUID;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.DraftPunishment;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Punishment;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
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
		byte[] address = TestingUtil.randomAddress();
		assertNull(core.getEnforcer().executeAndCheckConnection(uuid, "whoknows", address).join());
		Victim victim = PlayerVictim.of(uuid);
		testApplicable(victim, uuid, address);
	}
	
	private void testApplicableAddress() {
		UUID uuid = UUID.randomUUID();
		byte[] address = TestingUtil.randomAddress();
		assertNull(core.getEnforcer().executeAndCheckConnection(uuid, "another", address).join());
		Victim victim = AddressVictim.of(address);
		testApplicable(victim, uuid, address);
	}
	
	private void testApplicable(Victim victim, UUID uuid, byte[] address) {

		String reason = TestingUtil.randomString(TestingUtil.random().nextInt(100));
		DraftPunishment draft = new DraftPunishment.Builder().type(type).victim(victim)
				.operator(operator).reason(reason).scope(core.getScopeManager().globalScope()).build();
		Punishment enacted = core.getEnactor().enactPunishment(draft).join();
		assertNotNull(enacted, "Initial enactment failed for " + info);
		TestingUtil.assertEqualDetails(draft, enacted);

		if (type == PunishmentType.KICK) {
			// Kicks are always history, therefore never applicable, and cannot be undone
			return;
		}

		Punishment selected = core.getSelector().getApplicablePunishment(uuid, address, type).join();
		assertNotNull(selected, "Applicability selection failed for " + info);
		TestingUtil.assertEqualDetails(enacted, selected);

		testUndo(victim, enacted);
	}
	
	private void testUndo(Victim victim, Punishment enacted) {
		Random random = TestingUtil.random();
		if (random.nextBoolean()) {
			assertTrue(core.getEnactor().undoPunishment(enacted).join());
			return;
		}
		boolean singularAndChance = type.isSingular() && random.nextBoolean();
		if (random.nextBoolean()) {
			boolean result;
			if (singularAndChance) {
				result = core.getEnactor().undoPunishmentByTypeAndVictim(type, victim).join();
			} else {
				result = core.getEnactor().undoPunishmentByIdAndType(enacted.getID(), enacted.getType()).join();
			}
			assertTrue(result, "Undoing of enacted punishment failed for " + info);

		} else {

			Punishment retrieved;
			if (singularAndChance) {
				retrieved = core.getEnactor().undoAndGetPunishmentByTypeAndVictim(type, victim).join();
			} else {
				retrieved = core.getEnactor().undoAndGetPunishmentByIdAndType(enacted.getID(), enacted.getType()).join();
			}
			assertNotNull(retrieved, "Undoing and retrieving of enacted punishment failed for " + info);
			TestingUtil.assertEqualDetails(enacted, retrieved);
		}
	}
	
}
