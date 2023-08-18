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

package space.arim.libertybans.it.test.importing;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.EnforcementOptions;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.core.database.execute.QueryExecutor;
import space.arim.libertybans.core.importing.SelfImportProcess;
import space.arim.libertybans.core.punish.PunishmentCreator;
import space.arim.libertybans.it.DontInject;
import space.arim.libertybans.it.InjectionInvocationContextProvider;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static space.arim.libertybans.core.schema.Tables.ADDRESSES;
import static space.arim.libertybans.core.schema.Tables.BANS;
import static space.arim.libertybans.core.schema.Tables.HISTORY;
import static space.arim.libertybans.core.schema.Tables.NAMES;
import static space.arim.libertybans.core.schema.Tables.PUNISHMENTS;
import static space.arim.libertybans.core.schema.Tables.SIMPLE_ACTIVE;
import static space.arim.libertybans.core.schema.Tables.SIMPLE_HISTORY;
import static space.arim.libertybans.core.schema.Tables.VICTIMS;
import static space.arim.libertybans.core.schema.Tables.WARNS;

@ExtendWith(InjectionInvocationContextProvider.class)
public class SelfImportIT {

	private final SelfImportProcess selfImportProcess;
	private final Provider<QueryExecutor> queryExecutor;
	private final ScopeManager scopeManager;

	private SelfImportData selfImportData;

	@Inject
	public SelfImportIT(SelfImportProcess selfImportProcess, Provider<QueryExecutor> queryExecutor, 
						ScopeManager scopeManager) {
		this.selfImportProcess = selfImportProcess;
		this.queryExecutor = queryExecutor;
		this.scopeManager = scopeManager;
	}

	@BeforeEach
	public void setSelfImportData(@DontInject @TempDir Path temporaryFolder) {
		selfImportData = new SelfImportData(temporaryFolder);
	}

	private static void assertCount(DSLContext context, int startLine, int endLine, Table<?> table) {
		assertEquals(
				endLine - startLine + 1,
				context
						.selectCount()
						.from(table)
						.fetchSingle()
						.value1()
		);
	}

	@TestTemplate
	@Inject
	public void blueTree242(PunishmentCreator creator) throws IOException {
		Path folder = selfImportData.copyBlueTree242();

		assertDoesNotThrow(selfImportProcess.transferAllData(folder)::join);

		queryExecutor.get().execute((context) -> {
			// Assert the volume of data transferred, based on line numbers in the .script file
			assertCount(context, 18358, 27117, NAMES);
			assertCount(context, 27118, 36076, ADDRESSES);
			assertCount(context, 36077, 36330, PUNISHMENTS);
			assertCount(context, 36331, 36430, VICTIMS);
			assertCount(context, 36431, 36447, BANS);
			assertCount(context, 36448, 36451, WARNS);
			assertCount(context, 36452, 36705, HISTORY);

			// Assert some of the contents of the data
			assertEquals(
					Instant.ofEpochSecond(1637608075),
					context
							.select(NAMES.UPDATED)
							.from(NAMES)
							.where(NAMES.UUID.eq(UUID.fromString("000794e2-aaf8-3bd4-a1d0-2391f34bdcf0")))
							.and(NAMES.NAME.eq("McDown_pw_47Ie91"))
							.and(NAMES.LOWER_NAME.eq("mcdown_pw_47ie91"))
							.fetchOne(NAMES.UPDATED)
			);
			assertEquals(
					Instant.ofEpochSecond(1637608099),
					context
							.select(ADDRESSES.UPDATED)
							.from(ADDRESSES)
							.where(ADDRESSES.UUID.eq(UUID.fromString("0128c51e-62cb-32e8-8f8c-f92bebb27dec")))
							.and(ADDRESSES.ADDRESS.eq(addressUnchecked("212.154.251.210"))) // UNHEX(d49afbd2)
							.fetchOne(ADDRESSES.UPDATED)
			);
			assertEquals(
					creator.createPunishment(
							17L, PunishmentType.BAN, AddressVictim.of(addressUnchecked("80.100.23.146")),
							PlayerOperator.of(UUID.fromString("f360da52-6304-3af4-8b30-b7d9c5e6e162")), "swearing",
							scopeManager.globalScope(), Instant.ofEpochSecond(1636139831L), Instant.MAX, null
					),
					context
							.selectFrom(SIMPLE_ACTIVE)
							.where(SIMPLE_ACTIVE.ID.eq(17L))
							.fetchSingle(creator.punishmentMapper())
			);
			assertEquals(
					creator.createPunishment(
							44L, PunishmentType.WARN, PlayerVictim.of(UUID.fromString("ef1275f7-5c3a-36ed-92f6-6b3716c72896")),
							PlayerOperator.of(UUID.fromString("f360da52-6304-3af4-8b30-b7d9c5e6e162")), "abusing and getting items from creative",
							scopeManager.globalScope(), Instant.ofEpochSecond(1636916014L), Instant.MAX, null
					),
					context
							.selectFrom(SIMPLE_ACTIVE)
							.where(SIMPLE_ACTIVE.ID.eq(44L))
							.fetchSingle(creator.punishmentMapper())
			);
			assertEquals(
					creator.createPunishment(
							135L, PunishmentType.BAN, PlayerVictim.of(UUID.fromString("47df0fc2-3213-3401-af68-58cafb0e99f5")),
							ConsoleOperator.INSTANCE, "Everyone wants you banned, nerd",
							scopeManager.globalScope(), Instant.ofEpochSecond(1638635845L), Instant.ofEpochSecond(1639240645L), null
					),
					context
							.selectFrom(SIMPLE_HISTORY)
							.where(SIMPLE_HISTORY.ID.eq(135L))
							.fetchSingle(creator.punishmentMapper())
			);
		}).join();
	}

	@TestTemplate
	@Inject
	public void blueTree242sequences(PunishmentDrafter drafter) throws IOException {
		Path folder = selfImportData.copyBlueTree242();

		selfImportProcess.transferAllData(folder).join();

		queryExecutor.get().execute((context) -> {
			var banOfExistingPlayer = drafter.draftBuilder()
					.type(PunishmentType.BAN)
					.victim(PlayerVictim.of(UUID.fromString("47df0fc2-3213-3401-af68-58cafb0e99f5")))
					.reason("Banning an existing player to make sure punishment sequence is available")
					.build();
			var banOfNewPlayer = drafter.draftBuilder()
					.type(PunishmentType.BAN)
					.victim(PlayerVictim.of(UUID.randomUUID()))
					.reason("Banning a new player to make sure victim sequence is available")
					.build();

			EnforcementOptions noEnforcement = banOfExistingPlayer
					.enforcementOptionsBuilder()
					.enforcement(EnforcementOptions.Enforcement.NONE)
					.broadcasting(EnforcementOptions.Broadcasting.NONE)
					.build();
			banOfExistingPlayer
					.enactPunishment(noEnforcement)
					.toCompletableFuture().join()
					.orElseThrow();
			banOfNewPlayer
					.enactPunishment(noEnforcement)
					.toCompletableFuture().join()
					.orElseThrow();
		}).join();
	}

	private static NetworkAddress addressUnchecked(String address) {
		try {
			return NetworkAddress.of(InetAddress.getByName(address));
		} catch (UnknownHostException ex) {
			throw new AssertionError("Bad address: " + address, ex);
		}
	}
}
