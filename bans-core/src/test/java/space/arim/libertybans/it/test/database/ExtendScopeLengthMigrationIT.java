/*
 * LibertyBans
 * Copyright © 2026 Anand Beh
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

package space.arim.libertybans.it.test.database;

import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.bootstrap.BaseFoundation;
import space.arim.libertybans.core.database.flyway.MigrationTargetController;
import space.arim.libertybans.core.importing.SelfImportProcess;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.ThrowawayInstance;
import space.arim.libertybans.it.test.importing.SelfImportData;
import space.arim.libertybans.it.util.RandomUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static space.arim.libertybans.it.util.TestingUtil.assertEqualDetails;

/**
 * This test checks that the database-specific, scope column length-extending migrations perform successfully when
 * existing punishments with scopes exist.
 *
 */
@ExtendWith(InjectionInvocationContextProvider.class)
@ThrowawayInstance(manualStartup = true)
public class ExtendScopeLengthMigrationIT {

    private final BaseFoundation baseFoundation;
    private final MigrationTargetController migrationTargetController;

    public ExtendScopeLengthMigrationIT(BaseFoundation baseFoundation, MigrationTargetController migrationTargetController) {
        this.baseFoundation = baseFoundation;
        this.migrationTargetController = migrationTargetController;
    }

    @BeforeEach
    public void supplySampleData(SelfImportProcess selfImportProcess) throws IOException {
        migrationTargetController.setTarget(MigrationVersion.fromVersion("38"));
        baseFoundation.assertStartup();
        SelfImportData selfImportData = new SelfImportData(selfImportProcess.folder);
        Path dataSource = selfImportData.copyBlueTree242();
        selfImportProcess.transferAllData(dataSource).join();
    }

    @TestTemplate
    public void migrateServerScopesWithLongerLength(ScopeManager scopeManager, Guardian guardian,
                                                    PunishmentDrafter drafter, PunishmentSelector selector) {
        PlayerVictim victim1 = PlayerVictim.of(UUID.randomUUID());
        AddressVictim victim2 = AddressVictim.of(RandomUtil.randomAddress());
        ServerScope lobby = scopeManager.specificScope("lobby");
        ServerScope kitpvp = scopeManager.specificScope("kitpvp");
        Punishment ban1OnLobby = drafter
                .draftBuilder()
                .type(PunishmentType.BAN)
                .victim(victim1)
                .reason("no more lobbies")
                .scope(lobby)
                .build()
                .enactPunishment()
                .toCompletableFuture()
                .join()
                .orElseThrow();
        Punishment mute2OnKitpvp = drafter
                .draftBuilder()
                .type(PunishmentType.MUTE)
                .victim(victim2)
                .reason("muted for trash talking")
                .scope(kitpvp)
                .build()
                .enactPunishment()
                .toCompletableFuture()
                .join()
                .orElseThrow();

        // Update to latest version
        migrationTargetController.setTarget(null);
        assertTrue(assertDoesNotThrow(baseFoundation::fullRestart));

        // Should be able to retrieve same punishments, with same scopes
        {
            NetworkAddress victim1Addr = RandomUtil.randomAddress();
            assertNull(guardian.executeAndCheckConnection(victim1.getUUID(), "One", victim1Addr).join());

            Punishment retrieved = selector.selectionBuilder()
                    .victim(victim1)
                    .build()
                    .getFirstSpecificPunishment()
                    .toCompletableFuture()
                    .join()
                    .orElseThrow();
            assertEqualDetails(ban1OnLobby, retrieved);

            retrieved = selector.selectionByApplicabilityBuilder(victim1.getUUID(), victim1Addr)
                    .build()
                    .getFirstSpecificPunishment()
                    .toCompletableFuture()
                    .join()
                    .orElseThrow();
            assertEqualDetails(ban1OnLobby, retrieved);
        }
        UUID victim2UUID = UUID.randomUUID();
        assertNull(guardian.executeAndCheckConnection(victim2UUID, "Two", victim2.getAddress()).join());

        Punishment retrieved = selector.selectionBuilder()
                .victim(victim2)
                .build()
                .getFirstSpecificPunishment()
                .toCompletableFuture()
                .join()
                .orElseThrow();
        assertEqualDetails(mute2OnKitpvp, retrieved);
        retrieved = selector.selectionByApplicabilityBuilder(victim2UUID, victim2.getAddress())
                .build()
                .getFirstSpecificPunishment()
                .toCompletableFuture()
                .join()
                .orElseThrow();
        assertEqualDetails(mute2OnKitpvp, retrieved);
    }
}
