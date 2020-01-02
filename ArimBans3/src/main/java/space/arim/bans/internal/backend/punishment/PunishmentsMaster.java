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
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import space.arim.bans.api.CommandType;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.exception.InvalidUUIDException;
import space.arim.bans.api.exception.MissingPunishmentException;
import space.arim.bans.api.exception.TypeParseException;
import space.arim.bans.internal.Component;
import space.arim.bans.internal.sql.SelectionQuery;

public interface PunishmentsMaster extends Component {
	
	@Override
	default Class<?> getType() {
		return PunishmentsMaster.class;
	}
	
	int getNextAvailablePunishmentId();
	
	Punishment singleFromResultSet(ResultSet data) throws InvalidUUIDException, TypeParseException, SQLException;
	
	Set<Punishment> setFromResultSet(ResultSet data) throws InvalidUUIDException, TypeParseException, SQLException;
	
	List<Punishment> listFromResultSet(ResultSet data) throws InvalidUUIDException, TypeParseException, SQLException;
	
	Punishment firstFromQuery(SelectionQuery query) throws MissingPunishmentException;
	
	Set<Punishment> setFromQuery(SelectionQuery query);
	
	List<Punishment> listFromQuery(SelectionQuery query);
	
	List<Punishment> getForCmd(CommandType command, Subject target);
	
	void addPunishments(Punishment...punishments);
	
	void removePunishments(Punishment...punishments); 
	
	void changeReason(Punishment punishment, String reason);
	
	Punishment getPunishmentForSubjectAndType(Subject subject, PunishmentType type) throws MissingPunishmentException;
	
	Punishment getPunishmentById(int id) throws MissingPunishmentException;
	
	Set<Punishment> getPunishmentsForSubject(Subject subject);
	
	Set<Punishment> getPunishmentsByOperator(Subject operator);
	
}
