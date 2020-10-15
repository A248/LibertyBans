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
package space.arim.libertybans.api.select;

import java.util.UUID;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.api.NetworkAddress;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;

/**
 * A manager for selecting punishments with specific details from the database. This,
 * along with {@link SelectionOrder}, provide an API for efficient punishment queries. <br>
 * <br>
 * It is possible to select active and historical punishments. See
 * {@link space.arim.libertybans.api.punish} for a description of active and historical punishments.
 * 
 * @author A248
 *
 */
public interface PunishmentSelector {
	
	/**
	 * Begins creating a selection order by returning a {@link SelectionOrderBuilder}
	 * 
	 * @return a selection order builder
	 */
	SelectionOrderBuilder selectionBuilder();
	
	/**
	 * Gets an active punishment matching a specific ID, if a punishment with such ID exists. <br>
	 * <br>
	 * When the type of the punishment is known, {@link #getActivePunishmentByIdAndType(int, PunishmentType)}
	 * should be preferred.
	 * 
	 * @param id the punishment ID
	 * @return a future which yields the active punishment with the ID, or {@code null} if there is none
	 */
	CentralisedFuture<Punishment> getActivePunishmentById(int id);
	
	/**
	 * Gets an active punishment matching a specific ID and type, if one exists with matching type and ID. <br>
	 * <br>
	 * Unlike {@link #getActivePunishmentById(int)}, this method may be more efficient when the type of
	 * the punishment is known beforehand.
	 * 
	 * @param id the punishment ID
	 * @param type the punishment type
	 * @return a future which yields the active punishment with the ID and type, or {@code null} if there is none
	 */
	CentralisedFuture<Punishment> getActivePunishmentByIdAndType(int id, PunishmentType type);
	
	/**
	 * Gets a historical punishment matching a specific ID, if a punishment with such ID exists.
	 * 
	 * @param id the punishment ID
	 * @return a future which yields the historical punishment with the ID, or {@code null} if there is none
	 */
	CentralisedFuture<Punishment> getHistoricalPunishmentById(int id);

	/**
	 * Gets the first punishment, of a certain type, which is <i>applicable</i> to a UUID and IP address,
	 * where the UUID and address typically represent from connected player. <br>
	 * <br>
	 * Applicability is synonymous with enforceability. For example, if a ban would prevent a player from joining
	 * the server, the ban is said to be applicable to the player's UUID and IP. It may be, the player's IP is
	 * banned, the player's UUID is banned, or the player has played on a banned IP and that IP is banned while strict
	 * address enforcement is enabled. That is, this method takes into account configuration options related to enforcement.
	 * 
	 * @param uuid the player's UUID
	 * @param address the player's current address
	 * @param type the punishment type
	 * @return a future which yields the first applicable punishment for the id and address or {@code null} if there is none
	 */
	CentralisedFuture<Punishment> getApplicablePunishment(UUID uuid, NetworkAddress address, PunishmentType type);
	
	/**
	 * Gets a cached mute for an online player, including the player's UUID and address. <br>
	 * The mute will be <i>applicable</i> to the combination of UUID and address, see
	 * {@link #getApplicablePunishment(UUID, NetworkAddress, PunishmentType)} for a description of applicability. <br>
	 * <br>
	 * If the mute is cached, a completed future is returned. Else, an in-progress future is returned.
	 * 
	 * @param uuid the player's uuid
	 * @param address the player's current address
	 * @return a future yielding either a cached value or the queried mute
	 */
	CentralisedFuture<Punishment> getCachedMute(UUID uuid, NetworkAddress address);
	
}
