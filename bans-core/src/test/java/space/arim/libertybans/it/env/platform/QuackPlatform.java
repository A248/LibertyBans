/*
 * LibertyBans
 * Copyright © 2021 Anand Beh
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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.omnibus.util.ThisClass;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public class QuackPlatform {

	private final Map<UUID, QuackPlayer> players = new HashMap<>();

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	public QuackPlayer getPlayer(UUID uuid) {
		return players.get(uuid);
	}

	public QuackPlayer getPlayer(String name) {
		for (QuackPlayer player : players.values()) {
			if (player.getName().equalsIgnoreCase(name)) {
				return player;
			}
		}
		return null;
	}

	public Collection<? extends QuackPlayer> getAllPlayers() {
		return players.values();
	}

	void addPlayer(QuackPlayer player) {
		players.put(player.getUniqueId(), player);
	}

	void remove(QuackPlayer player, Component message) {
		players.values().remove(player);
		logger.info("{} was kicked for '{}'", player.getName(), toDisplay(message));
	}

	String toDisplay(Component message) {
		return PlainComponentSerializer.plain().serialize(message);
	}

}
