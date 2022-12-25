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

package space.arim.libertybans.it.test.applicable;

import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.it.env.platform.QuackPlatform;
import space.arim.libertybans.it.env.platform.QuackPlayer;
import space.arim.libertybans.it.env.platform.QuackPlayerBuilder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class StrictnessAssertHelper {

	private final PunishmentDrafter drafter;
	private final PunishmentSelector selector;
	private final Guardian guardian;
	private final QuackPlatform platform;

	@Inject
	public StrictnessAssertHelper(PunishmentDrafter drafter, PunishmentSelector selector,
								  Guardian guardian, QuackPlatform platform) {
		this.drafter = drafter;
		this.selector = selector;
		this.guardian = guardian;
		this.platform = platform;
	}

	private Component connectAndGetMessage(UUID uuid, String name, NetworkAddress address) {
		return guardian.executeAndCheckConnection(uuid, name, address).join();
	}

	public void connectAndAssertUnbannedUser(UUID uuid, String name, NetworkAddress address) {
		assertNull(connectAndGetMessage(uuid, name, address), "User " + uuid + "/" + name + " is not banned yet");
	}

	public void connectAndAssumeUnbannedUser(UUID uuid, String name, NetworkAddress address) {
		Component banMessage = connectAndGetMessage(uuid, name, address);
		assumeTrue(banMessage == null, "User " + uuid + "/" + name + " is not banned yet");
	}

	void banVictim(Victim victim, String reason) {
		Punishment punishment = drafter.draftBuilder()
				.type(PunishmentType.BAN)
				.victim(victim)
				.reason(reason)
				.build()
				.enactPunishment().toCompletableFuture().join().orElse(null);
		assertNotNull(punishment,
				"Conflicting punishment. Punishment reason = " + reason);
	}

	private Punishment getBan(UUID uuid, NetworkAddress address) {
		return selector
				.getApplicablePunishment(uuid, address, PunishmentType.BAN)
				.toCompletableFuture()
				.join().orElse(null);
	}

	void assertBanned(UUID uuid, NetworkAddress address, String assertion) {
		Punishment punishment = getBan(uuid, address);
		assertNotNull(punishment, assertion);

		QuackPlayer player = new QuackPlayerBuilder(platform).buildRandomName(uuid, address);
		punishment.enforcePunishment().toCompletableFuture().join();
		assertFalse(player.isStillOnline(), assertion + "; Player should have been kicked");
	}

	void assertNotBanned(UUID uuid, NetworkAddress address, String assertion) {
		assertNull(getBan(uuid, address), assertion);
	}

}
