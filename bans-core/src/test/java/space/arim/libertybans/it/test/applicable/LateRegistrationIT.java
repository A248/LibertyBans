/*
 * LibertyBans
 * Copyright © 2025 Anand Beh
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

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.api.select.AddressStrictness;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetAddressStrictness;
import space.arim.libertybans.it.SetAltRegistry;
import space.arim.libertybans.it.SetTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class is technically redundant, since the logic of late registration is covered through: <ul>
 *     <li>AccountHistoryIT, which checks for the late registration of account history</li>
 *     <li>The other ITs in this package, which use <code>@SetAltRegistry(all = true)</code> to ensure that punishments
 *     follow the same applicability rules, regardless of late registration</li>
 * </ul>
 *
 */
@ExtendWith(InjectionInvocationContextProvider.class)
public class LateRegistrationIT {

    @TestTemplate
    @SetAddressStrictness(AddressStrictness.NORMAL)
    @SetAltRegistry(SetAltRegistry.Option.ON_SERVER_SWITCH)
    // We use a consistent time in order to get the same punishment message
    @SetTime(unixTime = 1751274626)
    public void unregisteredAccountsDoNotBuildLinks(PunishmentDrafter drafter, Guardian guardian) {
        // If late registration is disabled, account history is built up every time someone connections
        // So, in this test, we check that new connections cannot cause existing accounts to become banned

        User guiltyUser = User.randomUser();
        User innocentUser = User.randomUser();
        User cleverHacker = new User(innocentUser.uuid(), guiltyUser.address());

        // In the beginning, both guiltyUser and innocentUser are free to join
        assertNull(guardian.executeAndCheckConnection(guiltyUser.uuid(), "GuiltyPlayer", guiltyUser.address()).join());
        assertNull(guardian.checkServerSwitch(guiltyUser.uuid(), "GuiltyPlayer", guiltyUser.address(), "please_register").join());
        assertNull(guardian.executeAndCheckConnection(innocentUser.uuid(), "InnocentPlayer", innocentUser.address()).join());
        assertNull(guardian.checkServerSwitch(innocentUser.uuid(), "InnocentPLayer", innocentUser.address(), "please_register").join());

        // Now add the ban
        drafter.draftBuilder()
                .type(PunishmentType.BAN)
                .victim(AddressVictim.of(guiltyUser.address()))
                .operator(ConsoleOperator.INSTANCE)
                .reason("Banned for life")
                .build()
                .enactPunishment()
                .toCompletableFuture()
                .join();

        // Both the banned player, and the clever alt, are banned
        Component banMsg = guardian.executeAndCheckConnection(
                guiltyUser.uuid(), "BannedPlayer", guiltyUser.address()
        ).join();
        assertNotNull(banMsg);

        Component stillBanMsg = guardian.executeAndCheckConnection(
                cleverHacker.uuid(), "CleverPlayer", cleverHacker.address()
        ).join();
        assertNotNull(stillBanMsg);
        assertEquals(banMsg, stillBanMsg);

        // However, the innocent player is still not banned
        assertNull(guardian.executeAndCheckConnection(innocentUser.uuid(), "NameChange", innocentUser.address()).join());
        assertNull(guardian.checkServerSwitch(innocentUser.uuid(), "NameChange", innocentUser.address(), "please_register").join());
    }
}
