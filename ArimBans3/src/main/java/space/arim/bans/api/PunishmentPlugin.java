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

import space.arim.registry.Registrable;

public interface PunishmentPlugin extends Registrable {
	
	boolean isBanned(Subject subject);
	
	boolean isMuted(Subject subject);
	
	Set<Punishment> getBanList();
	
	Set<Punishment> getMuteList();
	
	Set<Punishment> getWarns(Subject subject);
	
	Set<Punishment> getKicks(Subject subject);
	
	Set<Punishment> getHistory(Subject subject);
	
	void addPunishments(Punishment...punishments);
	
	void sendMessage(Subject subject, String message);
	
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
