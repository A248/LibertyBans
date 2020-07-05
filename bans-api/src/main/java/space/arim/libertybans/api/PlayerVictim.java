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

import java.util.UUID;

/**
 * A victim which is a player
 * 
 * @author A248
 *
 */
public class PlayerVictim extends Victim {

	private final UUID uuid;
	
	private PlayerVictim(UUID uuid) {
		super(VictimType.PLAYER);
		this.uuid = uuid;
	}
	
	/**
	 * Gets a victim for the specified UUID
	 * 
	 * @param uuid the player UUID
	 * @return the victim representation
	 */
	public static PlayerVictim of(UUID uuid) {
		return new PlayerVictim(uuid);
	}
	
	/**
	 * Gets the UUID of this victim
	 * 
	 * @return the player UUID
	 */
	public UUID getUUID() {
		return uuid;
	}

}
