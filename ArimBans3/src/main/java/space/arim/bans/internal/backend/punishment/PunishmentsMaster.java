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
package space.arim.bans.internal.backend.punishment;

import java.sql.ResultSet;
import java.util.Set;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.exception.ConflictingPunishmentException;
import space.arim.bans.api.exception.MissingPunishmentException;
import space.arim.bans.internal.Component;

public interface PunishmentsMaster extends Component {
	@Override
	default Class<?> getType() {
		return PunishmentsMaster.class;
	}
	
	void addPunishments(boolean async, Punishment...punishments) throws ConflictingPunishmentException;
	
	default void addPunishments(Punishment...punishments) throws ConflictingPunishmentException {
		addPunishments(true, punishments);
	}
	
	Punishment getPunishment(Subject subject, PunishmentType type) throws MissingPunishmentException;
	
	void removePunishments(boolean async, Punishment...punishments) throws MissingPunishmentException; 
	
	default void removePunishments(Punishment...punishments) throws MissingPunishmentException {
		removePunishments(true, punishments);
	}
	
	boolean hasPunishment(Subject subject, PunishmentType type);
	
	Set<Punishment> getPunishments(Subject subject);
	
	Set<Punishment> getPunishments(Subject subject, PunishmentType type);
	
	Set<Punishment> getAllPunishments();
	
	Set<Punishment> getAllPunishments(PunishmentType type);
	
	Set<Punishment> getHistory(Subject subject);
	
	Set<Punishment> getAllHistory();
	
	void loadActive(ResultSet data);
	
	void loadHistory(ResultSet data);
	
	void refreshActive();
}
