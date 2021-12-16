/* 
 * LibertyBans-api
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-api. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.api;

import java.util.Objects;
import java.util.UUID;

/**
 * A victim which is a player
 * 
 * @author A248
 *
 */
public final class PlayerVictim extends Victim {

	private final UUID uuid;

	private PlayerVictim(UUID uuid) {
		this.uuid = Objects.requireNonNull(uuid, "uuid");
	}

	/**
	 * Gets a victim from a UUID
	 * 
	 * @param uuid the player UUID
	 * @return a player victim
	 */
	public static PlayerVictim of(UUID uuid) {
		return new PlayerVictim(uuid);
	}
	
	/**
	 * Gets this victim's type: {@link VictimType#PLAYER}
	 * 
	 */
	@Override
	public VictimType getType() {
		return VictimType.PLAYER;
	}
	
	/**
	 * Gets the UUID of this victim
	 * 
	 * @return the player UUID
	 */
	public UUID getUUID() {
		return uuid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PlayerVictim that = (PlayerVictim) o;
		return uuid.equals(that.uuid);
	}

	@Override
	public int hashCode() {
		return uuid.hashCode();
	}

	@Override
	public String toString() {
		return "PlayerVictim{" +
				"uuid=" + uuid +
				'}';
	}
}
