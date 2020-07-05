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

import java.util.Set;
import java.util.UUID;

import space.arim.universal.util.concurrent.CentralisedFuture;

public interface PunishmentSelector {

	CentralisedFuture<Punishment> getFirstSpecificPunishment(PunishmentSelection selection);
	
	CentralisedFuture<Set<Punishment>> getSpecificPunishments(PunishmentSelection selection);
	
	CentralisedFuture<Punishment> getApplicablePunishment(UUID uuid, byte[] address, PunishmentType type);
	
	/**
	 * Gets all punishments regarding a specific {@link Victim}
	 * 
	 * @param victim the victim
	 * @return a future which yields all punishments applying to the victim, or an empty set
	 */
	CentralisedFuture<Set<Punishment>> getAllPunishmentsForVictim(Victim victim);
	
	/**
	 * Gets all applicable/enforceable punishments of a certain punishment type. <br>
	 * The punishment must not be expired and not undone.
	 * 
	 * @param type the punishment type
	 * @return a future which yields all applicable punishments matching the type, or an empty set
	 */
	CentralisedFuture<Set<Punishment>> getApplicablePunishmentsForType(PunishmentType type);

	/**
	 * Gets a cached mute for an online player, including the player's UUID and address. <br>
	 * This method takes into account strict address checking, if configured. <br>
	 * <br>
	 * If the mute is cached, a completed future is returned. Else, an in-progress future is returned.
	 * 
	 * @param uuid the player's uuid
	 * @param address the player's current address
	 * @return the completed future with the cached mute or an in-progress future if there was no cached entry
	 */
	CentralisedFuture<Punishment> getCachedMute(UUID uuid, byte[] address);
	
}
