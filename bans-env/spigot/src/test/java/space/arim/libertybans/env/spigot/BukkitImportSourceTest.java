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

package space.arim.libertybans.env.spigot;

import org.bukkit.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.core.importing.PortablePunishment;
import space.arim.omnibus.util.concurrent.FactoryOfTheFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentFactoryOfTheFuture;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static space.arim.libertybans.env.spigot.ServerWithBanListCreator.userBans;

@ExtendWith(MockitoExtension.class)
public class BukkitImportSourceTest {

	private ScopeManager scopeManager;

	@BeforeEach
	public void setScopeManager(@Mock ScopeManager scopeManager) {
		this.scopeManager = scopeManager;
		lenient().when(scopeManager.globalScope()).thenReturn(mock(ServerScope.class));
	}

	private Set<PortablePunishment> sourcePunishments(Server server) {
		FactoryOfTheFuture futuresFactory = new IndifferentFactoryOfTheFuture();
		return new BukkitImportSource(futuresFactory, scopeManager, server)
				.sourcePunishments().collect(Collectors.toUnmodifiableSet());
	}

	private void assertSourcePunishments(Set<PortablePunishment> expectedPunishments, Server server) {
		assertEquals(expectedPunishments, sourcePunishments(server));
	}

	@Test
	public void noBans() {
		assertSourcePunishments(Set.of(), userBans(Set.of()).ipBans(Set.of()));
	}

	private Instant currentTime() {
		// Must eliminate precision this way
		return java.util.Date.from(Instant.now()).toInstant();
	}

	@Test
	public void userBan() {
		String username = "lol_haha_dead";
		String reason = "for no reason";
		String operator = "A248";
		Instant start = currentTime();

		assertSourcePunishments(
				Set.of(new PortablePunishment(
						null,
						new PortablePunishment.KnownDetails(
								PunishmentType.BAN, reason, scopeManager.globalScope(),
								start, Punishment.PERMANENT_END_DATE),
						new PortablePunishment.VictimInfo(null, username, null),
						PortablePunishment.OperatorInfo.createUser(null, operator),
						true)),
				userBans(Set.of(
						SimpleBanEntry.forUser(username).reason(reason).created(start)
								.source(operator).build()
				)).ipBans(Set.of())
		);
	}

	@Test
	public void ipBan() throws UnknownHostException {
		InetAddress address = InetAddress.getByName("127.0.0.1");
		String reason = "for no reason";
		String operator = "A248";
		Instant start = currentTime();

		assertSourcePunishments(
				Set.of(new PortablePunishment(
						null,
						new PortablePunishment.KnownDetails(
								PunishmentType.BAN, reason, scopeManager.globalScope(),
								start, Punishment.PERMANENT_END_DATE),
						new PortablePunishment.VictimInfo(null, null, NetworkAddress.of(address)),
						PortablePunishment.OperatorInfo.createUser(null, operator),
						true)),
				userBans(Set.of()).ipBans(Set.of(
						SimpleBanEntry.forAddress(address).reason(reason).created(start)
								.source(operator).build()))
		);
	}

	@Test
	public void temporaryBan() {
		String username = "lol_haha_dead";
		String reason = "for no reason";
		String operator = "A248";
		Instant start = currentTime();
		Instant end = start.plus(Duration.ofDays(1L));

		assertSourcePunishments(
				Set.of(new PortablePunishment(
						null,
						new PortablePunishment.KnownDetails(
								PunishmentType.BAN, reason, scopeManager.globalScope(),
								start, end),
						new PortablePunishment.VictimInfo(null, username, null),
						PortablePunishment.OperatorInfo.createUser(null, operator),
						true)),
				userBans(Set.of(
						SimpleBanEntry.forUser(username).reason(reason).created(start)
								.source(operator).expiration(end).build()
				)).ipBans(Set.of())
		);
	}

}
