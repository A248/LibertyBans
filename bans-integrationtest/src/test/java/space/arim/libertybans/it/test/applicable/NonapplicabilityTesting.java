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

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.core.LibertyBansCore;
import space.arim.libertybans.it.util.TestingUtil;

public class NonapplicabilityTesting extends ApplicabilityTestingBase {

	public NonapplicabilityTesting(LibertyBansCore core, PunishmentType type, Operator operator) {
		super(core, type, operator);
	}
	
	@Override
	public void doTest() {
		final UUID uuid = UUID.randomUUID();
		final String name = TestingUtil.randomString(16);
		final byte[] address = TestingUtil.randomAddress();

		assertNull(core.getEnforcer().executeAndCheckConnection(uuid, name, address).join());
		assertNull(core.getEnforcer().checkChat(uuid, address, null).join());

		assertNull(core.getSelector().getApplicablePunishment(uuid, address, type).join());
		assertNull(core.getMuteCacher().getCachedMute(uuid, address).join());

		if (type.isSingular()) {
			for (Victim victim : new Victim[] {PlayerVictim.of(uuid), AddressVictim.of(address)}) {
				assertFalse(core.getEnactor().undoPunishmentByTypeAndVictim(type, victim).join());
				assertNull(core.getEnactor().undoAndGetPunishmentByTypeAndVictim(type, victim).join());
			}
		}
	}
	
}
