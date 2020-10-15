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
 * An operator which is a player
 * 
 * @author A248
 *
 */
public final class PlayerOperator extends Operator {
	
	private final UUID uuid;
	
	private PlayerOperator(UUID uuid) {
		this.uuid = uuid;
	}
	
	/**
	 * Gets an operator from a UUID
	 * 
	 * @param uuid the player UUID
	 * @return the operator representation of the player
	 */
	public static PlayerOperator of(UUID uuid) {
		return new PlayerOperator(Objects.requireNonNull(uuid, "uuid"));
	}
	
	/**
	 * Gets this operator's type: {@link OperatorType#PLAYER}
	 * 
	 */
	@Override
	public OperatorType getType() {
		return OperatorType.PLAYER;
	}
	
	/**
	 * Gets the UUID of this player operator
	 * 
	 * @return the uuid
	 */
	public UUID getUUID() {
		return uuid;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + uuid.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof PlayerOperator)) {
			return false;
		}
		PlayerOperator other = (PlayerOperator) object;
		return uuid.equals(other.uuid);
	}

	@Override
	public String toString() {
		return "PlayerOperator [uuid=" + uuid + "]";
	}

}
