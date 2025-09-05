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

package space.arim.libertybans.it.test.punish;

import jakarta.inject.Inject;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import space.arim.libertybans.api.AddressVictim;
import space.arim.libertybans.api.ConsoleOperator;
import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.PunishmentDrafter;
import space.arim.libertybans.core.env.InstanceType;
import space.arim.libertybans.core.scope.InternalScopeManager;
import space.arim.libertybans.it.InjectionInvocationContextProvider;
import space.arim.libertybans.it.PlatformSpecs;
import space.arim.libertybans.it.env.platform.QuackPlatform;
import space.arim.libertybans.it.env.platform.QuackPlayer;
import space.arim.libertybans.it.util.RandomUtil;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(InjectionInvocationContextProvider.class)
public class EnforcePerScopeIT {

    private final QuackPlatform platform;
    private final PunishmentDrafter drafter;
    private final InternalScopeManager scopeManager;

    private static final String GAMING = "gaming";
    private static final String LOBBY = "lobby";

    @Inject
    public EnforcePerScopeIT(QuackPlatform platform, PunishmentDrafter drafter, InternalScopeManager scopeManager) {
        this.platform = platform;
        this.drafter = drafter;
        this.scopeManager = scopeManager;
    }

    @TestTemplate
    @PlatformSpecs(instanceType = InstanceType.GAME_SERVER)
    public void banFromCurrentServer() {
        scopeManager.detectServerName(GAMING);

        QuackPlayer player = platform.newPlayer().buildFullyRandom();
        platform.assumeLogin(player);
        platform.assumeSendToServer(player, GAMING);
        drafter.draftBuilder()
                .type(PunishmentType.BAN)
                .victim(PlayerVictim.of(player.getUniqueId()))
                .operator(ConsoleOperator.INSTANCE)
                .reason("Banned on this server only")
                .scope(scopeManager.specificScope(GAMING))
                .build()
                .enactPunishment()
                .toCompletableFuture()
                .join().orElseThrow();
        // The player should have been kicked completely
        assertFalse(player.isOnline());
        // Also, they can't rejoin
        assertNotNull(platform.getEntryPoint().login(player));
    }

    @TestTemplate
    @PlatformSpecs(instanceType = InstanceType.GAME_SERVER)
    public void banFromOtherServer() {
        scopeManager.detectServerName(LOBBY);

        QuackPlayer player = platform.newPlayer().buildFullyRandom();
        platform.assumeLogin(player);

        drafter.draftBuilder()
                .type(PunishmentType.BAN)
                .victim(PlayerVictim.of(player.getUniqueId()))
                .operator(ConsoleOperator.INSTANCE)
                .reason("Banned on this server only")
                .scope(scopeManager.specificScope(GAMING))
                .build()
                .enactPunishment()
                .toCompletableFuture()
                .join().orElseThrow();
        // Player is unaffected
        assertTrue(player.isOnline());
    }

    @TestTemplate
    @PlatformSpecs(instanceType = InstanceType.PROXY)
    public void kickAddressFromSpecificServer() {
        NetworkAddress sharedAddress = RandomUtil.randomAddress();
        QuackPlayer onLobby = platform.newPlayer().buildRandomName(UUID.randomUUID(), sharedAddress);
        QuackPlayer onGame = platform.newPlayer().buildRandomName(UUID.randomUUID(), sharedAddress);
        platform.assumeLogin(onLobby);
        platform.assumeLogin(onGame);
        onLobby.setPlayableServerName(LOBBY);
        onGame.setPlayableServerName(GAMING);
        assertEquals(Set.of(onLobby, onGame), platform.getAllPlayers());

        drafter.draftBuilder()
                .type(PunishmentType.KICK)
                .victim(AddressVictim.of(sharedAddress))
                .operator(ConsoleOperator.INSTANCE)
                .reason("Banned on game only")
                .scope(scopeManager.specificScope(GAMING))
                .build()
                .enactPunishment()
                .toCompletableFuture()
                .join().orElseThrow();

        assertTrue(onLobby.isOnline());
        assertFalse(onGame.isOnline());
    }
}
