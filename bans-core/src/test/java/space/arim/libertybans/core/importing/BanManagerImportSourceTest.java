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
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.it.util.RandomUtil;
import space.arim.omnibus.util.UUIDUtil;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.quotedName;
import static org.jooq.impl.DSL.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static space.arim.libertybans.core.importing.BanManagerTable.IP_BANS;
import static space.arim.libertybans.core.importing.BanManagerTable.IP_BAN_RECORDS;

@ExtendWith(MockitoExtension.class)
@ExtendWith(LocalDatabaseSetup.class)
@LocalDatabaseSetup.H2
public class BanManagerImportSourceTest {

	private DSLContext context;
	private ImportSource importSource;

	private final UUID consoleUuid = UUID.randomUUID();
	private ServerScope globalScope;

	@BeforeEach
	public void setup(DSLContext context, ConnectionSource connectionSource) throws SQLException {
		this.context = context;

		ScopeManager scopeManager = mock(ScopeManager.class);
		globalScope = mock(ServerScope.class);
		lenient().when(scopeManager.globalScope()).thenReturn(globalScope);

		PluginDatabaseSetup pluginDatabaseSetup = new PluginDatabaseSetup(connectionSource);
		importSource = pluginDatabaseSetup.createBanManagerImportSource(scopeManager);

		pluginDatabaseSetup.initBanManagerSchema();
		pluginDatabaseSetup.addBanManagerConsole(consoleUuid);
	}

	private static Field<?> field(String fieldName) {
		return DSL.field(quotedName(fieldName));
	}

	private void insertBanManager(BanManagerTable table,
								  int id, Victim victim, String reason,
								  UUID operator, long time, long until) {
		boolean ipBased = victim instanceof AddressVictim;
		assert !ipBased || table.name().contains("IP");

		List<Field<?>> columns = new ArrayList<>(List.of(
				field("id"),
				field((ipBased) ? "ip" : "player_id"),
				field("reason"),
				field("actor_id"),
				field("created"),
				field(table.active ? "expires" : "expired")
		));
		List<Object> values = new ArrayList<>(List.of(
				id,
				(ipBased) ? ((AddressVictim) victim).getAddress().getRawAddress()
						: UUIDUtil.toByteArray(((PlayerVictim) victim).getUUID()),
				reason,
				UUIDUtil.toByteArray(operator),
				time,
				until
		));
		if (table.active) {
			columns.add(field("updated"));
			values.add(0L); // irrelevant
		} else {
			columns.addAll(List.of(
					field("pastActor_id"), field("pastCreated"), field("createdReason")
			));
			values.addAll(List.of(
					UUIDUtil.toByteArray(operator), time, "No reason for undoing"
			));
		}
		context.insertInto(table(quotedName(table.tableName("bm_"))))
				.columns(columns)
				.values(values)
				.execute();
	}

	@Test
	public void ipBans() {
		AddressVictim firstVictim = AddressVictim.of(RandomUtil.randomAddress());
		AddressVictim secondVictim = AddressVictim.of(RandomUtil.randomAddress());
		UUID secondOperator = UUID.randomUUID();
		insertBanManager(IP_BAN_RECORDS, 1, firstVictim, "ip banned", consoleUuid, 1642364163, 0L);
		insertBanManager(IP_BANS, 2, secondVictim, "player banned", secondOperator, 1642364025, 1642364025 + 1000L);

		assertEquals(
				Set.of(new PortablePunishment(
						1,
						new PortablePunishment.KnownDetails(
								PunishmentType.BAN, "ip banned", globalScope,
								Instant.ofEpochSecond(1642364163L), Punishment.PERMANENT_END_DATE),
						PortablePunishment.VictimInfo.simpleVictim(firstVictim),
						PortablePunishment.OperatorInfo.createConsole(),
						false
				), new PortablePunishment(
						2,
						new PortablePunishment.KnownDetails(
								PunishmentType.BAN, "player banned", globalScope,
								Instant.ofEpochSecond(1642364025L), Instant.ofEpochSecond(1642364025L + 1000L)),
						PortablePunishment.VictimInfo.simpleVictim(secondVictim),
						PortablePunishment.OperatorInfo.createUser(secondOperator, null),
						true
				)),
				importSource.sourcePunishments().collect(Collectors.toUnmodifiableSet())
		);
	}

	@Test
	public void namesAndAddresses() {
		UUID player = UUID.randomUUID();
		NetworkAddress address = RandomUtil.randomAddress();
		context.insertInto(table(quotedName("bm_players")))
				.columns(List.of(field("id"), field("name"), field("ip"), field("lastSeen")))
				.values(UUIDUtil.toByteArray(player), "PlayerName", address.getRawAddress(), 1642364025L)
				.execute();

		assertEquals(
				Set.of(new NameAddressRecord(player, "PlayerName", address, Instant.ofEpochSecond(1642364025L))),
				importSource.sourceNameAddressHistory().collect(Collectors.toUnmodifiableSet())
		);
	}
}
