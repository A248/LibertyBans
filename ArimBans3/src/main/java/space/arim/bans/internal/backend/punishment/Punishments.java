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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.Subject;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.exception.ConflictingPunishmentException;
import space.arim.bans.api.exception.InternalStateException;
import space.arim.bans.api.exception.MissingPunishmentException;
import space.arim.bans.internal.sql.SqlQuery;

public class Punishments implements PunishmentsMaster {
	
	private final ArimBans center;
	
	private int nextId = Integer.MIN_VALUE;
	private final Set<Punishment> active = ConcurrentHashMap.newKeySet();
	private final Set<Punishment> history = ConcurrentHashMap.newKeySet();

	public Punishments(ArimBans center) {
		this.center = center;
	}
	
	@Override
	public int getNextAvailablePunishmentId() {
		if (nextId == Integer.MIN_VALUE) {
			throw new InternalStateException("Invalid API call: ArimBans plugin data has not been loaded yet, so ArimBansLibrary#getNextAvailablePunishmentId() is inoperative until the plugin is fully loaded.");
		}
		return nextId++;
	}
	
	private void directAddPunishments(Punishment[] punishments) {
		// A set of queries we'll execute all together for increased efficiency
		Set<SqlQuery> exec = new HashSet<SqlQuery>();
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
		synchronized (active) {
			for (Punishment punishment : punishments) {

				// Check whether punishment is retrogade
				boolean retro = (punishment.expiration() > 0 && punishment.expiration() <= System.currentTimeMillis());

				// Call event before proceeding
				if (center.environment().enforcer().callPunishEvent(punishment, retro)) {

					// If it's retro we only need to add it to history
					// Otherwise we also need to add it to active
					if (history.add(punishment)) {
						exec.add(new SqlQuery(SqlQuery.Query.INSERT_HISTORY, punishment.type().deserialise(), punishment.subject().deserialise(), punishment.operator().deserialise(), punishment.expiration(), punishment.date()));
					}

					if (!retro && !punishment.type().equals(PunishmentType.KICK) && active.add(punishment)) { // add non-retro non-kick punishments to active
						exec.add(new SqlQuery(SqlQuery.Query.INSERT_ACTIVE, punishment.type().deserialise(), punishment.subject().deserialise(), punishment.operator().deserialise(), punishment.expiration(), punishment.date()));
					}

					// Add punishment to passedEvents so we can remember to call PostPunishEvents
					passedEvents.put(punishment, retro);
				}
			}
			// Execute queries
			center.sql().executeQuery((SqlQuery[]) exec.toArray());
		}

		// Call PostPunishEvents once done
		passedEvents.forEach(center.environment().enforcer()::callPostPunishEvent);
	}
	
	@Override
	public void addPunishments(Punishment... punishments) throws ConflictingPunishmentException {
		// Before proceeding, determine whether adding the specified punishments
		// would produce duplicate bans or mutes
		// If it would, throw an error terminating everything
		for (Punishment punishment : punishments) {
			if ((punishment.type().equals(PunishmentType.BAN) || punishment.type().equals(PunishmentType.MUTE)) && hasPunishment(punishment.subject(), punishment.type())) {
				throw new ConflictingPunishmentException(punishment.subject(), punishment.type());
			}
		}
		// Check whether we are already asynchronous. If not, run queries inside async.
		if (center.corresponder().asynchronous()) {
			directAddPunishments(punishments);
		} else {
			center.async(() -> directAddPunishments(punishments));
		}
	}
	
	@Override
	public Punishment getPunishment(Subject subject, PunishmentType type) throws MissingPunishmentException {
		Set<Punishment> active = getActive();
		for (Punishment punishment : active) {
			if (punishment.subject().equals(subject) && punishment.type().equals(type)) {
				return punishment;
			}
		}
		throw new MissingPunishmentException(subject, type);
	}
	
	private void directRemovePunishments(Punishment[] punishments) {
		// A set of queries we'll execute all together for increased efficiency
		Set<SqlQuery> exec = new HashSet<SqlQuery>();
		// A set of punishments for which UnpunishEvents were successfully called
		// At the end we'll call PostUnpunishEvent for each punishment in the set
		HashMap<Punishment, Boolean> passedEvents = new HashMap<Punishment, Boolean>();

		/* Synchronisation is needed here
		 * 
		 * For same reasons as stated under #directAddPunishments
		 * 
		 */
		synchronized (active) {
			for (Punishment punishment : punishments) {

				// Removal called in this method is never automatic
				boolean auto = false;
				
				// Call event before proceeding
				if (center.environment().enforcer().callUnpunishEvent(punishment, auto)) {
					if (active.remove(punishment)) {
						passedEvents.put(punishment, auto);
						exec.add(new SqlQuery(SqlQuery.Query.DELETE_ACTIVE_BY_ID, punishment.id()));
					}
				}
			}
			// Execute queries
			center.sql().executeQuery((SqlQuery[]) exec.toArray());

		}

		// Call PostUnpunishEvents once done
		passedEvents.forEach(center.environment().enforcer()::callPostUnpunishEvent);
	}
	
	@Override
	public void removePunishments(Punishment...punishments) throws MissingPunishmentException {
		for (Punishment punishment : punishments) {
			if (!active.contains(punishment)) {
				throw new MissingPunishmentException(punishment);
			}
		}
		// Check whether we are already asynchronous. If not, run queries inside async.
		if (center.corresponder().asynchronous()) {
			directRemovePunishments(punishments);
		} else {
			center.async(() -> directRemovePunishments(punishments));
		}
	}
	
	private void directChangeReason(Punishment punishment, String reason) {
		synchronized (active) {
			SqlQuery historyQuery = new SqlQuery(SqlQuery.Query.UPDATE_HISTORY_REASON_FOR_ID, reason, punishment.id());
			if (active.remove(punishment)) {
				history.remove(punishment);
				center.sql().executeQuery(historyQuery, new SqlQuery(SqlQuery.Query.UPDATE_ACTIVE_REASON_FOR_ID, reason, punishment.id()));
			} else {
				history.remove(punishment);
				center.sql().executeQuery(historyQuery);
			}
		}
	}
	
	@Override
	public void changeReason(Punishment punishment, String reason) throws MissingPunishmentException {
		if (!history.contains(punishment)) {
			throw new MissingPunishmentException(punishment);
		}
		// Check whether we are already asynchronous. If not, run queries async.
		if (center.corresponder().asynchronous()) {
			directChangeReason(punishment, reason);
		} else {
			center.async(() -> directChangeReason(punishment, reason));
		}
	}
	
	@Override
	public boolean hasPunishment(Subject subject, PunishmentType type) {
		for (Punishment punishment : active) {
			if (punishment.subject().equals(subject) && punishment.type().equals(type)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public Set<Punishment> getActive() {
		return Collections.unmodifiableSet(active());
	}
	
	@Override
	public Set<Punishment> getActiveCopy() {
		return new HashSet<Punishment>(active());
	}
	
	@Override
	public Set<Punishment> getHistory() {
		return Collections.unmodifiableSet(history);
	}
	
	@Override
	public Set<Punishment> getHistoryCopy() {
		return new HashSet<Punishment>(history);
	}
	
	@Override
	public void loadActive(ResultSet data) {
		try {
			while (data.next()) {
				active.add(new Punishment(data.getInt("id"), PunishmentType.serialise(data.getString("type")), Subject.serialise(data.getString("subject")), Subject.serialise(data.getString("operator")), data.getString("reason"), data.getLong("expiration"), data.getLong("date")));
			}
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
	}
	
	@Override
	public void loadHistory(ResultSet data) {
		try {
			int max = -1;
			while (data.next()) {
				int id = data.getInt("id");
				history.add(new Punishment(id, PunishmentType.serialise(data.getString("type")), Subject.serialise(data.getString("subject")), Subject.serialise(data.getString("operator")), data.getString("reason"), data.getLong("expiration"), data.getLong("date")));
				if (id > max) {
					max = id;
				}
			}
			nextId = ++max;
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
	}
	
	/**
	 * Returns THE set of active punishments, purging expired members.
	 * 
	 * <br><br><b>Changes are backed by the set because it is the same set!</b>
	 * 
	 * @return The set of active punishments
	 */
	private Set<Punishment> active() {
		HashMap<Punishment, Boolean> invalidated = new HashMap<Punishment, Boolean>();
		for (Iterator<Punishment> it = active.iterator(); it.hasNext();) {
			Punishment punishment = it.next();
			if (punishment.expiration() != -1L && punishment.expiration() < System.currentTimeMillis()) {
				
				// Removal called from this method is always automatic
				boolean auto = true;
				
				// call UnpunishEvent with parameter true because the removal is automatic
				if (center.environment().enforcer().callUnpunishEvent(punishment, auto)) {
					invalidated.put(punishment, auto);
					it.remove();
				}
			}
		}
		// Call PostUnpunishEvents before proceeding
		if (center.corresponder().asynchronous()) {
			invalidated.forEach(center.environment().enforcer()::callPostUnpunishEvent);
		} else {
			center.async(() -> invalidated.forEach(center.environment().enforcer()::callPostUnpunishEvent));
		}
		return active;
	}
	
	@Override
	public void refreshActive() {
		center.sql().executeQuery(new SqlQuery(SqlQuery.Query.REFRESH_ACTIVE));
	}
	
	@Override
	public void close() {
		refreshActive();
		active.clear();
		history.clear();
	}

}
