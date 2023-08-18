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

package space.arim.libertybans.core.importing;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.scope.ServerScope;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@ExtendWith(LocalDatabaseSetup.class)
@LocalDatabaseSetup.H2
public class LiteBansImportSourceTest {

	private ImportSource importSource;
	private DSLContext context;

	private ServerScope globalScope;
	private ServerScope kitpvpScope;
	private ServerScope lobbyScope;

	@BeforeEach
	public void setup(DSLContext context, ConnectionSource connectionSource) throws SQLException, IOException {
		this.context = context;

		globalScope = mock(ServerScope.class);
		kitpvpScope = mock(ServerScope.class);
		lobbyScope = mock(ServerScope.class);
		ScopeManager scopeManager = mock(ScopeManager.class);
		lenient().when(scopeManager.globalScope()).thenReturn(globalScope);
		lenient().when(scopeManager.specificScope("kitpvp")).thenReturn(kitpvpScope);
		lenient().when(scopeManager.specificScope("lobby")).thenReturn(lobbyScope);

		PluginDatabaseSetup pluginDatabaseSetup = new PluginDatabaseSetup(connectionSource);
		importSource = pluginDatabaseSetup.createLiteBansImportSource(scopeManager);

		pluginDatabaseSetup.initLiteBansSchema();
	}

	private Set<PortablePunishment> sourcePunishments() {
		return importSource.sourcePunishments().collect(Collectors.toUnmodifiableSet());
	}

	private void insertLiteBans(String table, String uuid, String ip, String reason,
								String bannedByUuid, String bannedByName, long time, long until,
								String scope, boolean ipBan) {
		context.insertInto(table("litebans_" + table))
				.columns(
						field("uuid"), field("ip"), field("reason"), field("banned_by_uuid"), field("banned_by_name"),
						field("time"), field("until"), field("server_scope"),
						field("active"), field("silent"), field("ipban"), field("ipban_wildcard"))
				.values(uuid, ip, reason, bannedByUuid, bannedByName, time, until, scope, true, false, ipBan, false)
				.execute();
	}

	@Test
	public void empty() {
		assertEquals(Set.of(), sourcePunishments());
	}

	@Test
	public void kicks() throws UnknownHostException {
		String uuidString = "ed5f12cd-6007-45d9-a4b9-940524ddaecf";
		UUID uuid = UUID.fromString(uuidString);
		String ipString = "173.79.156.19";
		NetworkAddress address = NetworkAddress.of(InetAddress.getByName(ipString));
		long kickOneDate = 1587336004000L;
		insertLiteBans(
				"kicks", uuidString, ipString, "test", "CONSOLE", "Console",
				kickOneDate, 0, "kitpvp", false);

		PortablePunishment kickByConsole = new PortablePunishment(1,
				new PortablePunishment.KnownDetails(
						PunishmentType.KICK, "test", kitpvpScope,
						Instant.ofEpochMilli(kickOneDate), Punishment.PERMANENT_END_DATE),
				new PortablePunishment.VictimInfo(uuid, null, address, PlayerVictim.of(uuid)),
				PortablePunishment.OperatorInfo.createConsole(),
				false);
		assertEquals(Set.of(kickByConsole), sourcePunishments());

		UUID ecotasticUUID = UUID.fromString("00140fe8-de08-41e9-ab79-e53b9f3f0fbd");
		long kickTwoDate = 1587336011000L;
		insertLiteBans(
				"kicks", uuidString, ipString, "test2",
				ecotasticUUID.toString(), "Ecotastic",
				kickTwoDate, 0, "*", false);
		PortablePunishment kickByEcotastic = new PortablePunishment(2,
				new PortablePunishment.KnownDetails(
						PunishmentType.KICK, "test2", globalScope,
						Instant.ofEpochMilli(kickTwoDate), Punishment.PERMANENT_END_DATE),
				new PortablePunishment.VictimInfo(uuid, null, address, PlayerVictim.of(uuid)),
				PortablePunishment.OperatorInfo.createUser(ecotasticUUID, "Ecotastic"),
				false);
		assertEquals(Set.of(kickByConsole, kickByEcotastic), sourcePunishments());
	}

	@Test
	public void ipBan() throws UnknownHostException {
		UUID victimUUID = UUID.fromString("d16bb58e-0307-4e4d-9e84-b96a825b08b0");
		String victimIp = "195.140.213.201";
		NetworkAddress victimAddress = NetworkAddress.of(InetAddress.getByName(victimIp));
		UUID ecotasticUUID = UUID.fromString("00140fe8-de08-41e9-ab79-e53b9f3f0fbd");
		long startTime = 1593013804802L;
		insertLiteBans(
				"bans", victimUUID.toString(), victimIp, "No reason stated.",
				ecotasticUUID.toString(), "Ecotastic", startTime, -1L, "lobby", true);
		PortablePunishment expectedPunishment = new PortablePunishment(1,
				new PortablePunishment.KnownDetails(
						PunishmentType.BAN, "No reason stated.", lobbyScope,
						Instant.ofEpochMilli(startTime), Punishment.PERMANENT_END_DATE),
				new PortablePunishment.VictimInfo(
						victimUUID, null, victimAddress, AddressVictim.of(victimAddress)),
				PortablePunishment.OperatorInfo.createUser(ecotasticUUID, "Ecotastic"),
				true);
		assertEquals(Set.of(expectedPunishment), sourcePunishments());
	}

	@Test
	public void undefinedIpAddressInHistory() {
		UUID uuid = UUID.fromString("a14a251d-9621-446a-9b8a-812a582c1245");
		String username = "SkyDoesMlnecraft";
		Instant date = Instant.parse("2021-07-21T01:59:32.000000Z");

		context.insertInto(table("litebans_history"))
				.columns(field("id"), field("date"), field("name"), field("uuid"), field("ip"))
				.values(30827, Timestamp.from(date), username, uuid.toString(), "#undefined#")
				.execute();

		assertEquals(Set.of(new NameAddressRecord(uuid, username, null, date)),
				importSource.sourceNameAddressHistory().collect(Collectors.toUnmodifiableSet()));
	}
}
