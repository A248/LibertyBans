package space.arim.bans.internal.backend.punishment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
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
	private ArimBans center;
	
	private Set<Punishment> active = ConcurrentHashMap.newKeySet();
	private Set<Punishment> history = ConcurrentHashMap.newKeySet();

	public Punishments(ArimBans center) {
		this.center = center;
	}
	
	private int punishmentCount(Subject subject, PunishmentType type) {
		int c = 0;
		Set<Punishment> a = active();
		for (Punishment p : a) {
			if (p.subject().compare(subject) && p.type().equals(type)) {
				c++;
			}
		}
		return c;
	}

	private void addPunishment(Punishment punishment) throws ConflictingPunishmentException {
		if ((punishment.type().equals(PunishmentType.BAN) || punishment.type().equals(PunishmentType.MUTE)) && punishmentCount(punishment.subject(), punishment.type()) > 0) {
			throw new ConflictingPunishmentException(punishment.subject(), PunishmentType.BAN);
		}
		center.async().execute(() -> {
			if (center.environment().enforcer().callPunishEvent(punishment)) {
				if (punishment.expiration() == -1L || punishment.expiration() > System.currentTimeMillis()) {
					active.add(punishment);
					history.add(punishment);
					center.sql().executeQuery(new SqlQuery(SqlQuery.Query.INSERT_ACTIVE.eval(center.sql().mode()), punishment.type().toString(), punishment.subject().deserialise(), punishment.operator().deserialise(), punishment.expiration(), punishment.date()), new SqlQuery(SqlQuery.Query.INSERT_HISTORY.eval(center.sql().mode()), punishment.type().toString(), punishment.subject().deserialise(), punishment.operator().deserialise(), punishment.expiration(), punishment.date()));
				} else {
					history.add(punishment);
					center.sql().executeQuery(new SqlQuery(SqlQuery.Query.INSERT_HISTORY.eval(center.sql().mode()), punishment.type().deserialise(), punishment.subject().deserialise(), punishment.operator().deserialise(), punishment.expiration(), punishment.date()));
				}
				
			}
		});
	}
	
	@Override
	public void addPunishments(Punishment... punishments) throws ConflictingPunishmentException {
		if (punishments.length == 1) {
			addPunishment(punishments[0]);
			return;
		}
		for (Punishment punishment : punishments) {
			if ((punishment.type().equals(PunishmentType.BAN) || punishment.type().equals(PunishmentType.MUTE)) && punishmentCount(punishment.subject(), punishment.type()) > 0) {
				throw new ConflictingPunishmentException(punishment.subject(), PunishmentType.BAN);
			}
		}
		center.async().execute(() -> {
			Set<SqlQuery> exec = new HashSet<SqlQuery>();
			for (Punishment punishment : punishments) {
				if (center.environment().enforcer().callPunishEvent(punishment)) {
					if (punishment.expiration() == -1L || punishment.expiration() > System.currentTimeMillis()) {
						active.add(punishment);
						history.add(punishment);
						exec.add(new SqlQuery(SqlQuery.Query.INSERT_ACTIVE.eval(center.sql().mode()), punishment.type().toString(), punishment.subject().deserialise(), punishment.operator().deserialise(), punishment.expiration(), punishment.date()));
						exec.add(new SqlQuery(SqlQuery.Query.INSERT_HISTORY.eval(center.sql().mode()), punishment.type().toString(), punishment.subject().deserialise(), punishment.operator().deserialise(), punishment.expiration(), punishment.date()));
					} else {
						history.add(punishment);
						exec.add(new SqlQuery(SqlQuery.Query.INSERT_HISTORY.eval(center.sql().mode()), punishment.type().deserialise(), punishment.subject().deserialise(), punishment.operator().deserialise(), punishment.expiration(), punishment.date()));
					}
				}
			}
			center.sql().executeQuery((SqlQuery[]) exec.toArray());
		});
	}
	
	@Override
	public Punishment getPunishment(Subject subject, PunishmentType type) throws MissingPunishmentException {
		Set<Punishment> a = active();
		for (Punishment p : a) {
			if (p.subject().compare(subject) && p.type().equals(type)) {
				return p;
			}
		}
		throw new MissingPunishmentException(subject, type);
	}
	
	@Override
	public void removePunishments(Punishment...punishments) throws MissingPunishmentException {
		for (Punishment punishment : punishments) {
			if (!active.contains(punishment)) {
				throw new MissingPunishmentException(punishment);
			}
		}
		center.async().execute(() -> {
			Set<SqlQuery> exec = new HashSet<SqlQuery>();
			for (Punishment punishment : punishments) {
				exec.add(new SqlQuery(SqlQuery.Query.DELETE_ACTIVE_FROM_DATE.eval(center.sql().mode()), punishment.date()));
			}
			center.sql().executeQuery((SqlQuery[]) exec.toArray());
		});
		synchronized (active) {
			active.removeAll(Arrays.asList(punishments));
		}

		
	}
	
	@Override
	public boolean isBanned(Subject subject) {
		return punishmentCount(subject, PunishmentType.BAN) > 0;
	}

	@Override
	public boolean isMuted(Subject subject) {
		return punishmentCount(subject, PunishmentType.MUTE) > 0;
	}

	@Override
	public Set<Punishment> getWarns(Subject subject) {
		Set<Punishment> warns = active();
		for (Iterator<Punishment> it = warns.iterator(); it.hasNext();) {
			Punishment punishment = it.next();
			if (!punishment.type().equals(PunishmentType.WARN) || !punishment.subject().compare(subject)) {
				it.remove();
			}
		}
		return warns;
	}
	
	@Override
	public void loadActive(ResultSet data) {
		try {
			while (data.next()) {
				active.add(new Punishment(PunishmentType.serialise(data.getString("type")), Subject.serialise(data.getString("subject")), Subject.serialise(data.getString("operator")), data.getString("reason"), data.getLong("expiration"), data.getLong("date")));
			}
		} catch (SQLException ex) {
			center.logError(ex);
		}
	}
	
	@Override
	public void loadHistory(ResultSet data) {
		try {
			while (data.next()) {
				history.add(new Punishment(PunishmentType.serialise(data.getString("type")), Subject.serialise(data.getString("subject")), Subject.serialise(data.getString("operator")), data.getString("reason"), data.getLong("expiration"), data.getLong("date")));
			}
		} catch (SQLException ex) {
			center.logError(ex);
		}
	}
	
	private Set<Punishment> active() {
		Set<Punishment> valid = new HashSet<Punishment>();
		synchronized (active) {
			for (Iterator<Punishment> it = active.iterator(); it.hasNext();) {
				Punishment punishment = it.next();
				if (punishment.expiration() != -1L && punishment.expiration() < System.currentTimeMillis()) {
					if (center.environment().enforcer().callUnpunishEvent(punishment, true)) {
						it.remove();
					}
				} else {
					valid.add(punishment);
				}
			}
		}
		return valid;
	}
	
	public void saveActive() {
		center.sql().executeQuery(SqlQuery.Query.REFRESH_ACTIVE.eval(center.sql().mode()));
	}
	
	public void saveHistory() {
		
	}
	
	@Override
	public void close() {
		active.clear();
		history.clear();
	}

	@Override
	public void refreshConfig() {
		
	}

}
