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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import space.arim.bans.ArimBans;
import space.arim.bans.api.CommandType;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.CommandType.IpSpec;
import space.arim.bans.api.CommandType.SubCategory;
import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.api.exception.InvalidUUIDException;
import space.arim.bans.api.exception.MissingPunishmentException;
import space.arim.bans.api.exception.TypeParseException;
import space.arim.bans.internal.sql.SelectionQuery;
import space.arim.bans.internal.sql.BasicQuery;
import space.arim.bans.internal.sql.BasicQuery.PreQuery;
import space.arim.bans.internal.sql.Query;

import space.arim.universal.util.collections.CollectionsUtil;

public class Punishments implements PunishmentsMaster {
	
	private final ArimBans center;
	
	private final AtomicInteger nextId = new AtomicInteger(Integer.MIN_VALUE);
	
	private final Object lock = new Object();

	public Punishments(ArimBans center) {
		this.center = center;
	}
	
	@Override
	public int getNextAvailablePunishmentId() {
		if (nextId.get() == Integer.MIN_VALUE) {
			throw new InternalStateException("Invalid API call: ArimBans plugin data has not been loaded yet, so ArimBansLibrary#getNextAvailablePunishmentId() is inoperative until the plugin is fully loaded.");
		}
		// return the value and then increment it
		return nextId.getAndIncrement();
	}
	
	@Override
	public Punishment singleFromResultSet(ResultSet data) throws InvalidUUIDException, TypeParseException, SQLException {
		return new Punishment(data.getInt("id"), PunishmentType.serialise(data.getString("type")), Subject.serialise(data.getString("subject")), Subject.serialise(data.getString("operator")), data.getString("reason"), data.getLong("expiration"), data.getLong("date"));
	}
	
	@Override
	public Set<Punishment> setFromResultSet(ResultSet data) throws InvalidUUIDException, TypeParseException, SQLException {
		Set<Punishment> punishments = new HashSet<Punishment>();
		while (data.next()) {
			punishments.add(singleFromResultSet(data));
		}
		return punishments;
	}
	
	@Override
	public List<Punishment> listFromResultSet(ResultSet data) throws InvalidUUIDException, TypeParseException, SQLException {
		List<Punishment> punishments = new ArrayList<Punishment>();
		while (data.next()) {
			punishments.add(singleFromResultSet(data));
		}
		return punishments;
	}
	
	@Override
	public Punishment firstFromQuery(SelectionQuery query) throws MissingPunishmentException {
		try (ResultSet data = center.sql().execute(query)[0]) {
			if (data.next()) {
				return singleFromResultSet(data);
			}
		} catch (InvalidUUIDException | TypeParseException | SQLException ex) {
			throw new MissingPunishmentException(ex);
		}
		throw new MissingPunishmentException();
	}
	
	@Override
	public Set<Punishment> setFromQuery(SelectionQuery query) {
		try (ResultSet data = center.sql().execute(query)[0]) {
			return setFromResultSet(data);
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
		return Collections.emptySet();
	}
	
	@Override
	public List<Punishment> listFromQuery(SelectionQuery query) {
		try (ResultSet data = center.sql().execute(query)[0]) {
			return listFromResultSet(data);
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
		return Collections.emptyList();
	}
	
	private SelectionQuery getQueryFilter(CommandType command, Subject target) {
		switch (command.subCategory()) {
		case BANLIST:
			return SelectionQuery.create("active").addCondition("type", PunishmentType.BAN);
		case MUTELIST:
			return SelectionQuery.create("active").addCondition("type", PunishmentType.MUTE);
		case WARNS:
			return SelectionQuery.create("active").addCondition("type", PunishmentType.WARN).addCondition("subject", target);
		case HISTORY:
			return SelectionQuery.create("history").addCondition("subject", target);
		case BLAME:
		case ROLLBACK:
			return SelectionQuery.create("active").addCondition("operator", target);
		default:
			return SelectionQuery.create("active");
		}
	}
	
	private Predicate<Punishment> getIpSpecFilter(CommandType command, Subject target) {
		if (command.subCategory().equals(SubCategory.BANLIST) || command.subCategory().equals(SubCategory.MUTELIST)) {
			return (punishment) -> command.ipSpec().equals(IpSpec.UUID) && !punishment.subject().getType().equals(Subject.SubjectType.PLAYER) || command.ipSpec().equals(IpSpec.IP) && !punishment.subject().getType().equals(Subject.SubjectType.IP);
		}
		return (punishment) -> false;
	}
	
	@Override
	public List<Punishment> getForCmd(CommandType command, Subject target) {
		try (ResultSet data = center.sql().execute(getQueryFilter(command, target))[0]) {
			List<Punishment> punishments = listFromResultSet(data);
			punishments.removeIf(getIpSpecFilter(command, target));
			return punishments;
		} catch (SQLException ex) {
			center.logs().logError(ex);
			return Collections.emptyList();
		}
	}
	
	private void directAddPunishments(Punishment[] punishments) {
		// A set of queries we'll execute all together for increased efficiency
		Set<Query> exec = new HashSet<Query>();
		// A map of punishments for which PunishEvents were successfully called
		// Key = the punishment, Value = whether it's retro
		// At the end we'll call PostPunishEvent for each punishment in the map
		HashMap<Punishment, Boolean> passedEvents = new HashMap<Punishment, Boolean>();
		/* Synchronisation is needed here
		 * 
		 * Otherwise, the following race condition may occur:
		 * 
		 * 1. Thread 1 - Add a punishment to local active set
		 * 2. Thread 2 - Remove the same punishment from local active set
		 * 3. Thread 2 - Attempt to query the remote database and remove the punishment
		 * 4. Thread 2 - SQLException because the punishment isn't yet in the remote database
		 * 5. Thread 1 - Add punishment to the remote database
		 * 6. Result - corrupted data, mismatch between local and remote data
		 * 
		 * It is HIGHLY unlikely that this condition will occur, but better safe than sorry.
		 * 
		 */
		synchronized (lock) {
			for (Punishment punishment : punishments) {
				// Check whether punishment is retrogade
				boolean retro = (punishment.expiration() > 0 && punishment.expiration() <= System.currentTimeMillis());
				if (center.corresponder().callPunishEvent(punishment, retro)) {
					exec.add(new BasicQuery(PreQuery.INSERT_HISTORY, punishment.type().deserialise(), punishment.subject().deserialise(), punishment.operator().deserialise(), punishment.expiration(), punishment.date()));
					if (!retro && !punishment.type().equals(PunishmentType.WARN)) {
						exec.add(new BasicQuery(PreQuery.INSERT_ACTIVE, punishment.type().deserialise(), punishment.subject().deserialise(), punishment.operator().deserialise(), punishment.expiration(), punishment.date()));
					}
					passedEvents.put(punishment, retro);
				}
			}
			// Execute queries
			center.sql().execute(exec);
		}
		// Call PostPunishEvents once done
		passedEvents.forEach(center.corresponder()::callPostPunishEvent);
	}
	
	@Override
	public void addPunishments(Punishment... punishments) {
		// Anti-synchronisation protection, for bad API calls
		// Check whether we are already asynchronous. If not, run queries inside async.
		if (center.getRegistry().getEvents().getUtil().isAsynchronous()) {
			directAddPunishments(punishments); // yay! all is good, API was used correctly
		} else { // uh-oh! potential for rare concurrency issues being silenced
			center.async(() -> directAddPunishments(punishments));
		}
	}
	
	private void directRemovePunishments(Punishment[] punishments) {
		// A set of punishments for which UnpunishEvents were successfully called
		// At the end we'll execute all the queries and then
		// we'll call PostUnpunishEvent for each punishment in the set
		Set<Punishment> passed = new HashSet<Punishment>();
		/* 
		 * Synchronisation is needed here
		 * 
		 * For same reasons as stated under #directAddPunishments
		 * 
		 */
		synchronized (lock) {
			for (Punishment punishment : punishments) {
				if (center.corresponder().callUnpunishEvent(punishment)) { // Call event before proceeding
					passed.add(punishment);
				}
			}
			// Execute queries
			center.sql().execute(CollectionsUtil.convertAll(passed.toArray(new Punishment[] {}), (punishment) -> new BasicQuery(PreQuery.DELETE_ACTIVE_BY_ID, punishment.id())));
		}
		// Call PostUnpunishEvents once done
		passed.forEach(center.corresponder()::callPostUnpunishEvent);
	}
	
	@Override
	public void removePunishments(Punishment...punishments) {
		// Anti-synchronisation protection, for bad API calls
		// Check whether we are already asynchronous. If not, run queries inside async.
		if (center.getRegistry().getEvents().getUtil().isAsynchronous()) { // yay! all is good, API was used correctly
			directRemovePunishments(punishments);
		} else { // uh-oh! potential for rare concurrency issues being silenced
			center.async(() -> directRemovePunishments(punishments));
		}
	}
	
	private void directChangeReason(Punishment punishment, String reason) {
		if (center.corresponder().callPunishmentChangeReasonEvent(punishment, reason)) {
			center.sql().execute(new BasicQuery(PreQuery.UPDATE_HISTORY_REASON_FOR_ID, punishment.id()), new BasicQuery(PreQuery.UPDATE_ACTIVE_REASON_FOR_ID, punishment.id()));
			center.corresponder().callPostPunishmentChangeReasonEvent(punishment, reason);
		}
	}
	
	@Override
	public void changeReason(Punishment punishment, String reason) {
		// Anti-synchronisation protection, for bad API calls
		// Check whether we are already asynchronous. If not, run queries async.
		if (center.getRegistry().getEvents().getUtil().isAsynchronous()) { // yay! all is good, API was used correctly
			directChangeReason(punishment, reason);
		} else { // uh-oh! potential for rare concurrency issues being silenced
			center.async(() -> directChangeReason(punishment, reason));
		}
	}
	
	@Override
	public Punishment getPunishmentForSubjectAndType(Subject subject, PunishmentType type) throws MissingPunishmentException {
		try (ResultSet data = center.sql().execute(SelectionQuery.create("active").addCondition("subject", subject).addCondition("type", type))[0]) {
			if (data.next()) {
				return singleFromResultSet(data);
			}
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
		throw new MissingPunishmentException(subject, type);
	}
	
	@Override
	public Punishment getPunishmentById(int id) throws MissingPunishmentException {
		try (ResultSet data = center.sql().execute(SelectionQuery.create("active").addCondition("id", id))[0]) {
			if (data.next()) {
				return singleFromResultSet(data);
			}
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
		throw new MissingPunishmentException(id);
	}
	
	@Override
	public Set<Punishment> getPunishmentsForSubject(Subject subject) {
		try (ResultSet data = center.sql().execute(SelectionQuery.create("active").addCondition("subject", subject))[0]) {
			return setFromResultSet(data);
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
		return Collections.emptySet();
	}
	
	@Override
	public Set<Punishment> getPunishmentsByOperator(Subject operator) {
		try (ResultSet data = center.sql().execute(SelectionQuery.create("active").addCondition("operator", operator))[0]) {
			return setFromResultSet(data);
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
		return Collections.emptySet();
	}
	
	@Override
	public void refreshConfig(boolean first) {
		if (first) {
			int id = -1;
			try (ResultSet data = center.sql().execute(SelectionQuery.create("active"))[0]) {
				Set<Punishment> punishments = setFromResultSet(data);
				for (Punishment punishment : punishments) {
					if (punishment.id() > id) {
						id = punishment.id();
					}
				}
			} catch (InvalidUUIDException | TypeParseException | SQLException ex) {
				center.logs().logError(ex);
			}
			nextId.set(++id);
		}
	}
}
