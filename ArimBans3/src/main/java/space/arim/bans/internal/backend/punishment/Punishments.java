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
import space.arim.bans.api.exception.MissingPunishmentException;
import space.arim.bans.internal.sql.SqlQuery;

public class Punishments implements PunishmentsMaster {
	
	private final ArimBans center;
	
	private int nextId = 0;
	private final Set<Punishment> active = ConcurrentHashMap.newKeySet();
	private final Set<Punishment> history = ConcurrentHashMap.newKeySet();

	public Punishments(ArimBans center) {
		this.center = center;
	}
	
	@Override
	public int getNextAvailablePunishmentId() {
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
					exec.add(new SqlQuery(SqlQuery.Query.INSERT_HISTORY, punishment.type().deserialise(), punishment.subject().deserialise(), punishment.operator().deserialise(), punishment.expiration(), punishment.date()));
					history.add(punishment);

					if (!retro) {
						active.add(punishment);
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
		passedEvents.forEach((punishment, retro) -> {
			center.environment().enforcer().callPostPunishEvent(punishment, retro);
		});
	}
	
	@Override
	public void addPunishments(boolean async, Punishment... punishments) throws ConflictingPunishmentException {
		// Before proceeding, determine whether adding the specified punishments
		// would produce duplicate bans or mutes
		// If it would, throw an error terminating everything
		for (Punishment punishment : punishments) {
			if ((punishment.type().equals(PunishmentType.BAN) || punishment.type().equals(PunishmentType.MUTE)) && hasPunishment(punishment.subject(), punishment.type())) {
				throw new ConflictingPunishmentException(punishment.subject(), punishment.type());
			}
		}
		// Check whether async execution was requested. If so, run queries inside async.
		if (async) {
			center.async(() -> directAddPunishments(punishments));
		} else {
			directAddPunishments(punishments);
		}
	}
	
	@Override
	public Punishment getPunishment(Subject subject, PunishmentType type) throws MissingPunishmentException {
		Set<Punishment> active = getAllPunishments();
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
		Set<Punishment> passedEvents = new HashSet<Punishment>();

		/* Synchronisation is needed here
		 * 
		 * For same explanation as stated under #directAddPunishments
		 * 
		 */
		synchronized (active) {
			for (Punishment punishment : punishments) {

				// Call event before proceeding
				if (center.environment().enforcer().callUnpunishEvent(punishment, false)) {

					passedEvents.add(punishment);
					exec.add(new SqlQuery(SqlQuery.Query.DELETE_ACTIVE_BY_ID, punishment.id()));
				
				}
			}
			// Execute queries
			center.sql().executeQuery((SqlQuery[]) exec.toArray());

			// Remove the punishments
			active.removeAll(passedEvents);
		}

		// Call PostUnpunishEvents once done
		passedEvents.forEach((punishment) -> {
			center.environment().enforcer().callPostUnpunishEvent(punishment, false);
		});
	}
	
	@Override
	public void removePunishments(boolean async, Punishment...punishments) throws MissingPunishmentException {
		for (Punishment punishment : punishments) {
			if (!active.contains(punishment)) {
				throw new MissingPunishmentException(punishment);
			}
		}
		// Check whether async execution was requested. If so, run queries inside async.
		if (async) {
			center.async(() -> directRemovePunishments(punishments));
		} else {
			directRemovePunishments(punishments);
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
	public Set<Punishment> getPunishments(Subject subject) {
		Set<Punishment> active = getAllPunishments();
		for (Iterator<Punishment> it = active.iterator(); it.hasNext();) {
			if (!it.next().subject().equals(subject)) {
				it.remove();
			}
		}
		return active;
	}
	
	@Override
	public Set<Punishment> getPunishments(Subject subject, PunishmentType type) {
		Set<Punishment> active = getAllPunishments();
		for (Iterator<Punishment> it = active.iterator(); it.hasNext();) {
			Punishment punishment = it.next();
			if (!punishment.subject().equals(subject) || !punishment.type().equals(type)) {
				it.remove();
			}
		}
		return active;
	}
	
	@Override
	public Set<Punishment> getAllPunishments() {
		return active();
	}
	
	@Override
	public Set<Punishment> getAllPunishments(PunishmentType type) {
		Set<Punishment> active = getAllPunishments();
		for (Iterator<Punishment> it = active.iterator(); it.hasNext();) {
			if (!it.next().type().equals(type)) {
				it.remove();
			}
		}
		return active;
	}
	
	@Override
	public Set<Punishment> getHistory(Subject subject) {
		Set<Punishment> history = getAllHistory();
		for (Iterator<Punishment> it = history.iterator(); it.hasNext();) {
			if (!it.next().subject().equals(subject)) {
				it.remove();
			}
		}
		return history;
	}
	
	@Override
	public Set<Punishment> getAllHistory() {
		return new HashSet<Punishment>(this.history);
	}
	
	@Override
	public void loadActive(ResultSet data) {
		try {
			int max = -1;
			while (data.next()) {
				int id = data.getInt("id");
				active.add(new Punishment(id, PunishmentType.serialise(data.getString("type")), Subject.serialise(data.getString("subject")), Subject.serialise(data.getString("operator")), data.getString("reason"), data.getLong("expiration"), data.getLong("date")));
				if (id > max) {
					max = id;
				}
			}
			nextId = ++max;
		} catch (SQLException ex) {
			center.logError(ex);
		}
	}
	
	@Override
	public void loadHistory(ResultSet data) {
		try {
			while (data.next()) {
				history.add(new Punishment(data.getInt("id"), PunishmentType.serialise(data.getString("type")), Subject.serialise(data.getString("subject")), Subject.serialise(data.getString("operator")), data.getString("reason"), data.getLong("expiration"), data.getLong("date")));
			}
		} catch (SQLException ex) {
			center.logError(ex);
		}
	}
	
	/**
	 * Returns a copy of the Set of active punishments,
	 * purging expired members.
	 * 
	 * <br><br>Changes are <b>NOT</b> backed by the set
	 * 
	 * @return Set of active punishments
	 */
	private Set<Punishment> active() {
		Set<Punishment> validated = new HashSet<Punishment>();
		Set<Punishment> invalidated = new HashSet<Punishment>();
		
		for (Iterator<Punishment> it = active.iterator(); it.hasNext();) {
			Punishment punishment = it.next();
			if (punishment.expiration() != -1L && punishment.expiration() < System.currentTimeMillis()) {
				// call UnpunishEvent with parameter true because the removal is automatic
				if (center.environment().enforcer().callUnpunishEvent(punishment, true)) {
					invalidated.add(punishment);
					it.remove();
				} else {
					validated.add(punishment);
				}
			} else {
				validated.add(punishment); // Seems a little redundant. Isn't there something I can use to avoid writing this twice?
			}
		}
		// Call PostUnpunishEvents before proceeding
		invalidated.forEach((punishment) -> {
			center.environment().enforcer().callPostUnpunishEvent(punishment, true);
		});
		return validated;
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
