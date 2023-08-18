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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.omnibus.util.UUIDUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@ExtendWith(LocalDatabaseSetup.class)
@LocalDatabaseSetup.Hsqldb
public class AdvancedBanImportSourceTest {

	private DSLContext context;
	private ImportSource importSource;

	private ServerScope globalScope;

	@BeforeEach
	public void setup(DSLContext context, ConnectionSource connectionSource) throws SQLException {
		this.context = context;

		ScopeManager scopeManager = mock(ScopeManager.class);
		globalScope = mock(ServerScope.class);
		lenient().when(scopeManager.globalScope()).thenReturn(globalScope);

		PluginDatabaseSetup pluginDatabaseSetup = new PluginDatabaseSetup(connectionSource);
		importSource = pluginDatabaseSetup.createAdvancedBanImportSource(scopeManager);

		pluginDatabaseSetup.initAdvancedBanSchema();
	}

	private void insertAdvancedBan(String table,
								   String name, String uuid, String reason, String operator,
								   String punishmentType, long startTime, long endTime) {
		context.insertInto(table(table))
				.columns(
						field("name"), field("uuid"), field("reason"), field("operator"),
						field("punishmentType"), field("start"), field("end"), field("calculation")
				)
				.values(name, uuid, reason, operator, punishmentType, startTime, endTime, null)
				.execute();
	}

	private void insertActiveNoCopyToHistory(String name, String uuid, String reason, String operator,
											 String punishmentType, long startTime, long endTime) {
		insertAdvancedBan("Punishments", name, uuid, reason, operator, punishmentType, startTime, endTime);
	}

	private void insertHistory(String name, String uuid, String reason, String operator,
							   String punishmentType, long startTime, long endTime) {
		insertAdvancedBan("PunishmentHistory", name, uuid, reason, operator, punishmentType, startTime, endTime);
	}

	private void insertActive(String name, String uuid, String reason, String operator,
							  String punishmentType, long startTime, long endTime) {
		insertHistory(name, uuid, reason, operator, punishmentType, startTime, endTime);
		insertActiveNoCopyToHistory(name, uuid, reason, operator, punishmentType, startTime, endTime);
	}

	private Set<PortablePunishment> sourcePunishments() {
		return importSource.sourcePunishments().collect(Collectors.toUnmodifiableSet());
	}

	@Test
	public void empty() {
		assertEquals(Set.of(), sourcePunishments());
	}

	@ParameterizedTest
	@ValueSource(strings= {"BAN", "TEMP_BAN"})
	public void activeReplacesHistorical(String punishmentType) {
		String uuidString = "394aef836e3f482b9988933cabe3b1cd";
		UUID uuid = UUIDUtil.fromShortString(uuidString);
		long startTime = System.currentTimeMillis();
		long endTime = startTime - 1000L;
		insertHistory(
				"creeperfreddy", uuidString, "bug exploit",
				"CONSOLE", punishmentType, startTime, endTime);

		PortablePunishment historicalPunishment = new PortablePunishment(0,
				new PortablePunishment.KnownDetails(
						PunishmentType.BAN, "bug exploit", globalScope,
						Instant.ofEpochMilli(startTime), Instant.ofEpochMilli(endTime)),
				new PortablePunishment.VictimInfo(uuid, "creeperfreddy", null),
				PortablePunishment.OperatorInfo.createConsole(),
				false);
		assertEquals(Set.of(historicalPunishment), sourcePunishments());

		/*
		 * Insert identical active punishment, and verify replacement in stream
		 */

		insertActiveNoCopyToHistory(
				"creeperfreddy", uuidString, "bug exploit",
				"CONSOLE", punishmentType, startTime, endTime);

		PortablePunishment activePunishment = new PortablePunishment(0,
				historicalPunishment.knownDetails(),
				historicalPunishment.victimInfo(),
				historicalPunishment.operatorInfo(),
				true);
		assertEquals(Set.of(activePunishment), sourcePunishments());
	}

	@Test
	public void skipNotes() {
		String uuidString = "00140fe8de0841e9ab79e53b9f3f0fbd";
		long startTime = System.currentTimeMillis();
		long endTime = startTime - 1000L;
		insertActive(
				"Ecotastic", uuidString, "noteworthy",
				"A248", "NOTE", startTime, endTime);
		assertEquals(Set.of(), sourcePunishments());
	}

	@Test
	public void missingUniqueId() {
		insertActive("Zalliah", null, "Banned for using Kill Aura",
				"Ecotastic", "BAN", 0, System.currentTimeMillis());
		assertThrows(ThirdPartyCorruptDataException.class, this::sourcePunishments);
	}

	@Test
	public void ipBan() throws UnknownHostException {
		String victimName = "MrBunnyBear";
		String victimIp = "195.140.213.201";
		long startTime = 1593013804802L;
		insertActive(victimName, victimIp, "No reason stated.",
				"Ecotastic", "IP_BAN", startTime, -1L);
		PortablePunishment expectedPunishment = new PortablePunishment(0,
				new PortablePunishment.KnownDetails(
						PunishmentType.BAN, "No reason stated.", globalScope,
						Instant.ofEpochMilli(startTime), Punishment.PERMANENT_END_DATE),
				new PortablePunishment.VictimInfo(
						null, victimName, NetworkAddress.of(InetAddress.getByName(victimIp))),
				PortablePunishment.OperatorInfo.createUser(null, "Ecotastic"),
				true);
		assertEquals(Set.of(expectedPunishment), sourcePunishments());
	}

	@Test
	public void namesInUuidColumn() {
		// AdvancedBan uses names in the uuid column on offline-mode servers
		String victimName = "Whoever";
		String reason = "Some reason";
		long startTime = 1593013804802L;
		insertActive(victimName, victimName, reason, "Ecotastic", "BAN", startTime, -1L);
		PortablePunishment expectedPunishment = new PortablePunishment(0,
				new PortablePunishment.KnownDetails(
						PunishmentType.BAN, reason, globalScope,
						Instant.ofEpochMilli(startTime), Punishment.PERMANENT_END_DATE),
				new PortablePunishment.VictimInfo(null, victimName, null),
				PortablePunishment.OperatorInfo.createUser(null, "Ecotastic"),
				true);
		assertEquals(Set.of(expectedPunishment), sourcePunishments());
	}

}
