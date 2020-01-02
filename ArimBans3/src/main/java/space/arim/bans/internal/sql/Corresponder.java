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
package space.arim.bans.internal.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentResult;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.events.PostPunishEvent;
import space.arim.bans.api.events.PostPunishmentChangeReasonEvent;
import space.arim.bans.api.events.PostUnpunishEvent;
import space.arim.bans.api.events.PunishEvent;
import space.arim.bans.api.events.PunishmentChangeReasonEvent;
import space.arim.bans.api.events.UnpunishEvent;
import space.arim.bans.api.exception.InvalidUUIDException;
import space.arim.bans.api.exception.TypeParseException;
import space.arim.bans.internal.backend.resolver.CacheElement;

public class Corresponder implements CorresponderMaster {

	private final ArimBans center;
	
	private boolean strict_ip_checking;
	
	public Corresponder(ArimBans center) {
		this.center = center;
	}
	
	private PunishmentResult fromPunishmentAndData(Subject subject, ResultSet data) throws InvalidUUIDException, TypeParseException, SQLException {
		Punishment punishment = center.punishments().singleFromResultSet(data);
		return new PunishmentResult(subject, punishment, center.formats().formatPunishment(punishment));
	}
	
	@Override
	public PunishmentResult getApplicablePunishment(UUID uuid, String address, PunishmentType type) {
		Subject subject = center.subjects().parseSubject(uuid);
		SelectionQuery punishmentQuery = SelectionQuery.create("active").addCondition("subject", subject).addCondition("type", type);
		ResultSet[] data;
		try {
			if (strict_ip_checking) {
				data = center.sql().execute(punishmentQuery, SelectionQuery.create("cache").addCondition("uuid", uuid));
				if (data[0].next()) {
					return fromPunishmentAndData(subject, data[0]);
				} else if (data[1].next()) {
					CacheElement cache = center.resolver().singleFromResultSet(data[1]);
					Set<String> ips = cache.getIps();
					if (!ips.contains(address)) {
						ips.add(address);
					}
					Set<Query> exec = new HashSet<Query>();
					ips.forEach((addr) -> exec.add(SelectionQuery.create("active").addCondition("subject", center.subjects().parseSubject(addr, false)).addCondition("type", type)));
					if (!cache.hasIp(address)) {
						exec.add(cache.addIp(address));
					}
					ResultSet[] data2 = center.sql().execute(exec);
					for (int n = 0; n < ips.size(); n++) {
						if (data2[n].next()) {
							return fromPunishmentAndData(subject, data2[n]);
						}
					}
				}
			} else {
				Subject addrSubj = center.subjects().parseSubject(address, false);
				data = center.sql().execute(punishmentQuery, SelectionQuery.create("active").addCondition("subject", addrSubj));
				if (data[0].next()) {
					return fromPunishmentAndData(subject, data[0]);
				} else if (data[1].next()) {
					return fromPunishmentAndData(subject, data[1]);
				}
			}
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
		return new PunishmentResult();
	}
	
	@Override
	public Set<Punishment> getApplicablePunishments(UUID uuid, String address, PunishmentType type) {
		Subject subject = center.subjects().parseSubject(uuid);
		SelectionQuery punishmentQuery = SelectionQuery.create("active").addCondition("subject", subject).addCondition("type", type);
		ResultSet[] data;
		try {
			if (strict_ip_checking) {
				data = center.sql().execute(punishmentQuery, SelectionQuery.create("cache").addCondition("uuid", uuid));
				Set<Punishment> possible = center.punishments().setFromResultSet(data[0]);
				if (!possible.isEmpty()) {
					return possible;
				}
				if (data[1].next()) {
					CacheElement cache = center.resolver().singleFromResultSet(data[1]);
					Set<String> ips = cache.getIps();
					if (!ips.contains(address)) {
						ips.add(address);
					}
					Set<Query> exec = new HashSet<Query>();
					ips.forEach((addr) -> exec.add(SelectionQuery.create("active").addCondition("subject", center.subjects().parseSubject(addr, false)).addCondition("type", type)));
					if (!cache.hasIp(address)) {
						exec.add(cache.addIp(address));
					}
					ResultSet[] data2 = center.sql().execute(exec);
					for (int n = 0; n < ips.size(); n++) {
						Set<Punishment> possible2 = center.punishments().setFromResultSet(data2[n]);
						if (!possible2.isEmpty()) {
							return possible2;
						}
					}
				}
			} else {
				Subject addrSubj = center.subjects().parseSubject(address, false);
				data = center.sql().execute(punishmentQuery, SelectionQuery.create("active").addCondition("subject", addrSubj).addCondition("type", type));
				Set<Punishment> possible = center.punishments().setFromResultSet(data[0]);
				if (!possible.isEmpty()) {
					return possible;
				}
				Set<Punishment> possible2 = center.punishments().setFromResultSet(data[1]);
				if (!possible2.isEmpty()) {
					return possible2;
				}
			}
		} catch (SQLException ex) {
			center.logs().logError(ex);
		}
		return Collections.emptySet();
	}
	
	@Override
	public void enact(Punishment punishment, boolean add, Subject operator) {
		if (!punishment.silent()) {
			center.subjects().sendNotif(punishment, add, operator);
		}
		if (!punishment.passive() && add) {
			center.environment().enforce(punishment, center.formats().useJson());
		}
		center.logs().log(Level.FINE, "Operator " + operator.toString() + ((add) ? " punished " : " unpunished ") + punishment.subject().toString() + ". Punishment details: " + punishment.toString());
	}
	
	@Override
	public boolean callPunishEvent(Punishment punishment, boolean retro) {
		return center.getRegistry().getEvents().fireEvent(new PunishEvent(punishment, retro));
	}
	
	@Override
	public boolean callUnpunishEvent(Punishment punishment) {
		return center.getRegistry().getEvents().fireEvent(new UnpunishEvent(punishment));
	}
	
	@Override
	public void callPostPunishEvent(Punishment punishment, boolean retro) {
		center.getRegistry().getEvents().fireEvent(new PostPunishEvent(punishment, retro));
	}
	
	@Override
	public void callPostUnpunishEvent(Punishment punishment) {
		center.getRegistry().getEvents().fireEvent(new PostUnpunishEvent(punishment));
	}
	
	@Override
	public boolean callPunishmentChangeReasonEvent(Punishment punishment, String reason) {
		return center.getRegistry().getEvents().fireEvent(new PunishmentChangeReasonEvent(punishment, reason));
	}
	
	@Override
	public void callPostPunishmentChangeReasonEvent(Punishment punishment, String reason) {
		center.getRegistry().getEvents().fireEvent(new PostPunishmentChangeReasonEvent(punishment, reason));
	}
	
	@Override
	public void refreshConfig(boolean first) {
		strict_ip_checking = center.config().getConfigBoolean("enforcement.strict-ip-checking");
	}
	
}
