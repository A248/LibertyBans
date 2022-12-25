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
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.PunishmentRevoker;
import space.arim.libertybans.api.punish.RevocationOrder;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetAddressStrictness;
import space.arim.libertybans.it.resolver.RandomPunishmentTypeResolver;
import space.arim.libertybans.it.util.RandomUtil;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(InjectionInvocationContextProvider.class)
@ExtendWith(RandomPunishmentTypeResolver.class)
public class NotPunishedIT {

	private final PunishmentSelector selector;
	private final PunishmentRevoker revoker;
	private final Guardian guardian;
	private final PunishmentType type;

	@Inject
	public NotPunishedIT(PunishmentSelector selector, PunishmentRevoker revoker, Guardian guardian,
			@DontInject PunishmentType type) {
		this.selector = selector;
		this.revoker = revoker;
		this.guardian = guardian;
		this.type = type;
	}

	@TestTemplate
	@SetAddressStrictness(all = true)
	public void testNotPunished() {
		final UUID uuid = UUID.randomUUID();
		final String name = RandomUtil.randomString(16);
		final NetworkAddress address = RandomUtil.randomAddress();

		assertNull(guardian.executeAndCheckConnection(uuid, name, address).join());
		assertNull(guardian.checkChat(uuid, address, null).join());

		assertNull(selector.getApplicablePunishment(uuid, address, type).toCompletableFuture().join().orElse(null));
		assertNull(selector.getCachedMute(uuid, address).toCompletableFuture().join().orElse(null));

		if (type.isSingular()) {
			for (Victim victim : new Victim[] {PlayerVictim.of(uuid), AddressVictim.of(address)}) {
				RevocationOrder order = revoker.revokeByTypeAndVictim(type, victim);
				assertFalse(order.undoPunishment().toCompletableFuture().join());
				assertNull(order.undoAndGetPunishment().toCompletableFuture().join().orElse(null));
			}
		}
	}

}
