/* 
 * ArimBansLib, an API for ArimBans
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansLib. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import space.arim.bans.api.exception.ConflictingPunishmentException;
import space.arim.bans.api.exception.MissingPunishmentException;

import space.arim.universal.registry.Registrable;
import space.arim.universal.registry.UniversalRegistry;

/**
 * A generic punishment plugin for developers who wish to use the {@link Punishment}
 * 
 * @author A248
 *
 */
public interface PunishmentPlugin extends Registrable {
	
	/**
	 * Retrieves the {@link UniversalRegistry} instance associated with this plugin. <br>
	 * <br>
	 * The corresponding events manager, retrievable with {@link UniversalRegistry#getEvents()}, should be used for event listening and firing.
	 * 
	 * @return a UniversalRegistry instance
	 */
	default UniversalRegistry getRegistry() {
		return UniversalRegistry.get();
	}
	
	/**
	 * The next available punishment ID. <br>
	 * <br>
	 * To prevent ID conflicts, this method should be used for Punishment construction.
	 * 
	 * @return the next ID
	 */
	int getNextAvailablePunishmentId();
	
	/**
	 * Gets a possible ban for a player and address
	 * 
	 * @param uuid the player's {@link UUID}
	 * @param address the player's IP address
	 * @return a {@link PunishmentResult} with a possible ban
	 */
	PunishmentResult getApplicableBan(UUID uuid, String address);
	
	/**
	 * Gets a possible mute for a player and address
	 * 
	 * @param uuid the player's {@link UUID}
	 * @param address the player's IP address
	 * @return a {@link PunishmentResult} with a possible mute
	 */
	PunishmentResult getApplicableMute(UUID uuid, String address);
	
	/**
	 * Gets all warns for a player and address
	 * 
	 * @param uuid the player's {@link UUID}
	 * @param address the player's IP address
	 * @return set of warns, empty if none found
	 */
	Set<Punishment> getApplicableWarns(UUID uuid, String address);
	
	/**
	 * Gets all kicks for a player and address
	 * 
	 * @param uuid the player's {@link UUID}
	 * @param address the player's IP address
	 * @return set of kicks, empty if none found
	 */
	Set<Punishment> getApplicableKicks(UUID uuid, String address);
	
	/**
	 * Returns an umodifiable active punishments set
	 * 
	 * @return a backed set of active punishments
	 */
	Set<Punishment> getActivePunishments();
	
	/**
	 * A copy of the active punishments
	 * 
	 * @return a deep clone of the set of the active punishments
	 */
	default Set<Punishment> getActivePunishmentsCopy() {
		return new HashSet<Punishment>(getActivePunishments());
	}
	
	/**
	 * Returns an unmodifiable punishment history set
	 * 
	 * @return a backed set of historical punishments
	 */
	Set<Punishment> getHistoryPunishments();
	
	/**
	 * A copy of punishment history
	 * 
	 * @return a deep clone of the set of historical punishments
	 */
	default Set<Punishment> getHistoryPunishmentsCopy() {
		return new HashSet<Punishment>(getHistoryPunishments());
	}
	
	/**
	 * Adds punishments to the backend server.
	 * 
	 * @param punishments the punishments to add
	 * @throws ConflictingPunishmentException if punishments conflict in ID or type
	 */
	void addPunishments(Punishment...punishments) throws ConflictingPunishmentException;
	
	/**
	 * Removes punishments from the backend server
	 * 
	 * @param punishments the punishments to remove
	 * @throws MissingPunishmentException if any of the punishments aren't found
	 */
	void removePunishments(Punishment...punishments) throws MissingPunishmentException;
	
	/**
	 * Sends a message to a player by his/her UUID
	 * 
	 * @param player the uuid of the target player
	 * @param messages the content to send
	 */
	void sendMessage(UUID player, String...messages);
	
	/**
	 * Sends a message to a Subject, parsing potential Json messages if enabled
	 * 
	 * @param subject the target of the message
	 * @param messages the content to send
	 */
	void sendMessage(Subject subject, String...messages);
	
	/**
	 * Returns the server console Subject
	 * 
	 * <br><b>Careful!</b> The console has unbounded permissions.
	 * 
	 * @return a Subject representing the console
	 */
	default Subject getConsole() {
		return Subject.console();
	}
	
}
