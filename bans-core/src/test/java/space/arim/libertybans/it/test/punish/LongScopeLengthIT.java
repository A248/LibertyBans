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

package space.arim.libertybans.it.test.punish;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.scope.ScopeManager;
import space.arim.libertybans.api.scope.ServerScope;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.libertybans.core.scope.SpecificServerScope;
import space.arim.libertybans.it.InjectionInvocationContextProvider;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static space.arim.libertybans.it.util.TestingUtil.assertEqualDetails;

@ExtendWith(InjectionInvocationContextProvider.class)
public class LongScopeLengthIT {

    private static final String LONG_SCOPE = UUID.randomUUID().toString();

    @BeforeAll
    public static void checkLong() {
        assert LONG_SCOPE.length() > 32;
    }

    @TestTemplate
    public void insertAndRetrieve(PunishmentDrafter drafter, ScopeManager scopeManager, PunishmentSelector selector) {
        Victim sampleVictim = PlayerVictim.of(UUID.randomUUID());
        Punishment initial = drafter
                .draftBuilder()
                .type(PunishmentType.BAN)
                .victim(sampleVictim)
                .operator(ConsoleOperator.INSTANCE)
                .reason("because I said so")
                .scope(scopeManager.specificScope(LONG_SCOPE))
                .build()
                .enactPunishment()
                .toCompletableFuture()
                .join()
                .orElseThrow();

        List<Punishment> history = selector
                .selectionBuilder()
                .victim(sampleVictim)
                .selectAll()
                .build()
                .getAllSpecificPunishments()
                .toCompletableFuture()
                .join();
        assertEquals(1, history.size(), () -> "Received history of " + history);
        Punishment singlePunishment = history.get(0);
        assertEqualDetails(initial, singlePunishment);

        ServerScope scope = singlePunishment.getScope();
        assertEquals(LONG_SCOPE, ((SpecificServerScope) scope).server());
    }
}
