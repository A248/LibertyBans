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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.InetAddress;
import java.util.UUID;

import jakarta.inject.Inject;

import space.arim.api.chat.SendableMessage;

import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.punish.Enforcer;

public class StrictnessAssertHelper {

	private final PunishmentDrafter drafter;
	private final PunishmentSelector selector;
	private final Enforcer enforcer;

	@Inject
	public StrictnessAssertHelper(PunishmentDrafter drafter, PunishmentSelector selector, Enforcer enforcer) {
		this.drafter = drafter;
		this.selector = selector;
		this.enforcer = enforcer;
	}

	private SendableMessage connectAndGetMessage(UUID uuid, String name, InetAddress address) {
		return enforcer.executeAndCheckConnection(uuid, name, address).join();
	}

	void connectAndAssertUnbannedUser(UUID uuid, String name, InetAddress address) {
		assertNull(connectAndGetMessage(uuid, name, address), "User " + uuid + "/" + name + " is not banned yet");
	}

	void connectAndAssumeUnbannedUser(UUID uuid, String name, InetAddress address) {
		SendableMessage banMessage = connectAndGetMessage(uuid, name, address);
		assumeTrue(banMessage == null, "User " + uuid + "/" + name + " is not banned yet");
	}

	void banAddress(InetAddress address, String reason) {
		Punishment punishment = drafter.draftBuilder()
				.type(PunishmentType.BAN)
				.victim(AddressVictim.of(address))
				.reason(reason)
				.build()
				.enactPunishment().toCompletableFuture().join().orElse(null);
		assertNotNull(punishment,
				"Conflicting punishment. Punishment reason = " + reason);
	}

	private Punishment getBan(UUID uuid, InetAddress address) {
		return selector.getApplicablePunishment(uuid, NetworkAddress.of(address), PunishmentType.BAN)
				.toCompletableFuture().join().orElse(null);
	}

	void assertBanned(UUID uuid, InetAddress address, String assertion) {
		assertNotNull(getBan(uuid, address), assertion);
	}

	void assertNotBanned(UUID uuid, InetAddress address, String assertion) {
		assertNull(getBan(uuid, address), assertion);
	}

}
