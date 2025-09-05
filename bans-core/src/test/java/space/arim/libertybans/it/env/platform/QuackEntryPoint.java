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

package space.arim.libertybans.it.env.platform;

import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import space.arim.libertybans.core.selector.Guardian;
import space.arim.libertybans.it.env.QuackEnforcer;

public final class QuackEntryPoint {

    private final Guardian guardian;
    private final QuackPlayerStore playerStore;
    private final QuackEnforcer quackEnforcer;

    @Inject
    public QuackEntryPoint(Guardian guardian, QuackPlayerStore playerStore, QuackEnforcer quackEnforcer) {
        this.guardian = guardian;
        this.playerStore = playerStore;
        this.quackEnforcer = quackEnforcer;
    }

    /// Attempts to perform login, or returns kick message
    public @Nullable Component login(QuackPlayer player) {
        Component denyMessage = guardian.executeAndCheckConnection(
                player.getUniqueId(), player.getName(), player.getAddress()
        ).join();
        if (denyMessage == null) {
            guardian.onJoin(player, quackEnforcer);
            playerStore.insert(player);
        }
        return denyMessage;
    }

    /// Attempts to perform connection to server, or returns kick message
    public @Nullable Component sendToServer(QuackPlayer player, String server) {
        Component message = guardian.checkServerSwitch(
                player.getUniqueId(), player.getName(), player.getAddress(), server
        ).join();
        if (message != null) {
            player.setPlayableServerName(server);
        }
        return message;
    }
}
