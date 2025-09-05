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
import space.arim.libertybans.core.env.EnvUserResolver;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.SetAddressStrictness;
import space.arim.libertybans.it.SetAltRegistry;
import space.arim.libertybans.it.SetTime;
import space.arim.libertybans.it.env.platform.QuackPlatform;
import space.arim.libertybans.it.env.platform.QuackPlayer;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(InjectionInvocationContextProvider.class)
public class LateRegistrationIT {

    /**
     * Covers the logic of late registration, preventing account history from being registered until server switch
     * occurs.
     * <p>
     * This test is technically redundant, since the logic of late registration is covered through: <ul>
     *     <li>AccountHistoryIT, which checks for the late registration of account history</li>
     *     <li>The other ITs in this package, which use <code>@SetAltRegistry(all = true)</code> to ensure that punishments
     *     follow the same applicability rules, regardless of late registration</li>
     * </ul>
     *
     * @param drafter the drafter, injected
     * @param guardian the guardian, injected
     */
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
        assertNull(guardian.checkServerSwitch(innocentUser.uuid(), "InnocentPlayer", innocentUser.address(), "please_register").join());

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

    @TestTemplate
    @SetAddressStrictness({AddressStrictness.NORMAL, AddressStrictness.STERN, AddressStrictness.STRICT})
    @SetAltRegistry(SetAltRegistry.Option.ON_SERVER_SWITCH)
    public void banPlayerWhileInLimbo(PunishmentDrafter drafter, QuackPlatform platform,
                                      EnvUserResolver envUserResolver) {

        User user = User.randomUser();
        QuackPlayer player = platform.newPlayer().buildRandomName(user.uuid(), user.address());

        // At first, the player joins the proxy - but is not registered yet
        platform.assumeLogin(player);

        // Next, they're banned by IP address
        assertEquals(Optional.of(player.getAddress()), envUserResolver.lookupAddress(player.getName()).join());
        drafter.draftBuilder()
                .type(PunishmentType.BAN)
                .victim(AddressVictim.of(user.address()))
                .operator(ConsoleOperator.INSTANCE)
                .reason("Banned for life")
                .build()
                .enactPunishment()
                .toCompletableFuture()
                .join();

        // Now, they should have been ejected from the proxy
        assertFalse(player.isOnline());
    }
}
