/*
 * ArimBans, a punishment plugin for minecraft servers
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.api;

import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Subject.SubjectType;
import space.arim.bans.api.exception.ConflictingPunishmentException;
import space.arim.bans.api.exception.PlayerNotFoundException;

public class ArimBansLibrary {
	
	private ArimBans center;
	
	public ArimBansLibrary(ArimBans center) {
		this.center = center;
	}
	
	public boolean isBanned(Subject subject) {
		return center.punishments().isBanned(subject);
	}
	
	public boolean isMuted(Subject subject) {
		return center.punishments().isMuted(subject);
	}
	
	public int countWarns(Subject subject) {
		return getWarns(subject).size();
	}
	
	public Set<Punishment> getWarns(Subject subject) {
		return center.punishments().getWarns(subject);
	}
	
	public Set<Punishment> getKicks(Subject subject) {
		return center.punishments().getKicks(subject);
	}
	
	public void addPunishments(Punishment...punishments) throws ConflictingPunishmentException {
		center.punishments().addPunishments(punishments);
	}
	
	public Subject fromUUID(UUID subject) {
		return center.subjects().parseSubject(subject);
	}
	
	public Subject fromPlayer(Player subject) {
		return center.subjects().parseSubject(subject.getUniqueId());
	}
	
	/**
	 * Gets a Subject from an IP Address
	 * 
	 * @param address - the address to use
	 * @return a Subject representing the address
	 * @throws IllegalArgumentException if address is invalid according to ApacheCommons InetAddressValidator
	 */
	public Subject fromIpAddress(String address) throws IllegalArgumentException {
		Subject subject = center.subjects().parseSubject(address);
		if (subject.getType().equals(SubjectType.IP)) {
			return subject;
		}
		throw new IllegalArgumentException("Could not make " + address + " into a subject because it is not a valid IP address! To avoid this error, surround your API call in a try/catch statement.");
	}
	
	/**
	 * Returns the console
	 * 
	 * <br><b>Careful!</b> The console has unbounded permissions.
	 * 
	 * @return Subject
	 */
	public Subject getConsole() {
		return Subject.console();
	}
	
	public void simulateCommand(Subject subject, CommandType command, String[] args) {
		center.commands().execute(subject, command, args);
	}
	
	/**
	 * Checks that a UUID corresponds to a cached player
	 * 
	 * @param uuid - the uuid to be checked
	 * @return true if player's UUID is cached
	 */
	public boolean checkUUID(UUID uuid) {
		return center.subjects().checkUUID(uuid);
	}
	
	/**
	 * Check whether a UUID corresponds to any player
	 * Differs from {@link #checkUUID(UUID)} in that this method will query the Ashcon/Mojang APIs.
	 * 
	 * <br><br><b>This is a blocking operation</b>
	 * 
	 * @param uuid - the uuid to be checked
	 * @return
	 */
	public boolean lookupUUID(UUID uuid) {
		try {
			center.environment().resolver().nameFromUUID(uuid);
			return true;
		} catch (PlayerNotFoundException ex) {
			
		}
		return false;
	}
	
	public boolean checkAddress(String address) {
		return Tools.validAddress(address);
	}

}
