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

import java.net.InetAddress;
import java.util.UUID;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.Operator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.revoke.RevocationOrder;
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
		final InetAddress address = TestingUtil.randomAddress();

		assertNull(core.getEnforcementCenter().executeAndCheckConnection(uuid, name, address).join());
		assertNull(core.getEnforcementCenter().checkChat(uuid, address, null).join());

		NetworkAddress netAddress = NetworkAddress.of(address);
		assertNull(core.getSelector().getApplicablePunishment(uuid, netAddress, type).join());
		assertNull(core.getSelector().getCachedMute(uuid, netAddress).join());

		if (type.isSingular()) {
			for (Victim victim : new Victim[] {PlayerVictim.of(uuid), AddressVictim.of(address)}) {
				RevocationOrder order = core.getRevoker().revokeByTypeAndVictim(type, victim);
				assertFalse(order.undoPunishment().join());
				assertNull(order.undoAndGetPunishment().join());
			}
		}
	}
	
}
