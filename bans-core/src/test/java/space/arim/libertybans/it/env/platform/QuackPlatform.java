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
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assumptions.abort;

public final class QuackPlatform {

	private final QuackPlayerStore playerStore;
	private final QuackEntryPoint entryPoint;

	@Inject
    public QuackPlatform(QuackPlayerStore playerStore, QuackEntryPoint entryPoint) {
        this.playerStore = playerStore;
        this.entryPoint = entryPoint;
    }

	static String toDisplay(Component message) {
		return PlainComponentSerializer.plain().serialize(message);
	}

	public QuackPlayerStore getPlayerStore() {
		return playerStore;
	}

	public QuackEntryPoint getEntryPoint() {
		return entryPoint;
	}

	public QuackPlayerBuilder newPlayer() {
		return new QuackPlayerBuilder(this);
	}

	public void assumeLogin(QuackPlayer player) {
        Component denyMessage = getEntryPoint().login(player);
        if (denyMessage != null) {
            abort("Denied with reason " + toDisplay(denyMessage));
        }
	}

    public void assumeSendToServer(QuackPlayer player, String server) {
        Component denyMessage = getEntryPoint().sendToServer(player, server);
        if (denyMessage != null) {
            abort("Denied with reason " + toDisplay(denyMessage));
        }
    }

	public Set<? extends QuackPlayer> getAllPlayers() {
		return new HashSet<>(playerStore.getAllPlayers());
	}
}
