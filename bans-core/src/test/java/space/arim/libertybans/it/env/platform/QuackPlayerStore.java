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

import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public interface QuackPlayerStore {

    Optional<QuackPlayer> getPlayer(UUID uuid);

    Optional<QuackPlayer> getPlayer(String name);

    Collection<? extends QuackPlayer> getAllPlayers();

    /// Inserts a player into the store, bypassing the login process
    void insert(QuackPlayer player);

    void remove(QuackPlayer player);

    void removeAll();

    @Singleton
    class Impl implements QuackPlayerStore {

        private final Map<UUID, QuackPlayer> players = new ConcurrentHashMap<>();

        public Optional<QuackPlayer> getPlayer(UUID uuid) {
            return Optional.ofNullable(players.get(uuid));
        }

        public Optional<QuackPlayer> getPlayer(String name) {
            for (QuackPlayer player : players.values()) {
                if (player.getName().equalsIgnoreCase(name)) {
                    return Optional.of(player);
                }
            }
            return Optional.empty();
        }

        public Collection<? extends QuackPlayer> getAllPlayers() {
            return players.values();
        }

        @Override
        public void insert(QuackPlayer player) {
            QuackPlayer previous = players.putIfAbsent(player.getUniqueId(), player);
            if (previous != null) {
                throw new UnsupportedOperationException(
                        "Player with this UUID already exists. Tried to add " + player + " but found " + previous);
            }
        }

        @Override
        public void remove(QuackPlayer player) {
            players.remove(player.getUniqueId());
        }

        @Override
        public void removeAll() {
            players.clear();
        }

    }
}
