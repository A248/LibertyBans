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
import space.arim.registry.Registrable;

public interface PunishmentPlugin extends Registrable {
	
	PunishmentResult getApplicableBan(UUID uuid, String address);
	
	PunishmentResult getApplicableMute(UUID uuid, String address);
	
	Set<Punishment> getApplicableWarns(UUID uuid, String address);
	
	Set<Punishment> getApplicableKicks(UUID uuid, String address);
	
	Set<Punishment> getActivePunishments();
	
	default Set<Punishment> getActivePunishmentsCopy() {
		return new HashSet<Punishment>(getActivePunishments());
	}
	
	Set<Punishment> getHistoryPunishments();
	
	default Set<Punishment> getHistoryPunishmentsCopy() {
		return new HashSet<Punishment>(getHistoryPunishments());
	}
	
	void addPunishments(Punishment...punishments) throws ConflictingPunishmentException;
	
	void removePunishments(Punishment...punishments) throws MissingPunishmentException;
	
	void sendMessage(Subject subject, String...messages);
	
	void sendMessage(UUID player, String...messages);
	
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
