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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentResult;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.Subject.SubjectType;
import space.arim.bans.api.exception.MissingCacheException;
import space.arim.bans.api.exception.MissingPunishmentException;

public class Corresponder implements CorresponderMaster {

	private final ArimBans center;
	
	private final long mainThread;
	
	private boolean strict_ip_checking;
	
	public Corresponder(ArimBans center) {
		this.center = center;
		mainThread = Thread.currentThread().getId();
	}
	
	@Override
	public boolean asynchronous() {
		return Thread.currentThread().getId() != mainThread;
	}
	
	@Override
	public PunishmentResult getApplicablePunishment(UUID uuid, String address, PunishmentType type) {
		Subject subject = center.subjects().parseSubject(uuid);
		if (center.punishments().hasPunishment(subject, type)) {
			try {
				Punishment punishment = center.punishments().getPunishment(subject, type);
				return new PunishmentResult(subject, punishment, center.formats().formatPunishment(punishment));
			} catch (MissingPunishmentException ex) {}
		} else if (strict_ip_checking) {
			List<String> ips = (address != null) ? new ArrayList<String>(Arrays.asList(address)) : new ArrayList<String>();
			try {
				ips.addAll(center.resolver().getIps(uuid));
			} catch (MissingCacheException ex) {}
			for (String addr : ips) {
				Subject addrSubj = center.subjects().parseSubject(addr, false);
				if (center.punishments().hasPunishment(addrSubj, type)) {
					try {
						Punishment punishment = center.punishments().getPunishment(addrSubj, type);
						return new PunishmentResult(subject, punishment, center.formats().formatPunishment(punishment));
					} catch (MissingPunishmentException ex) {}
				}
			}
		} else if (address != null) {
			Subject addrSubj = center.subjects().parseSubject(address, false);
			if (center.punishments().hasPunishment(addrSubj, type)) {
				try {
					Punishment punishment = center.punishments().getPunishment(addrSubj, type);
					return new PunishmentResult(subject, punishment, center.formats().formatPunishment(punishment));
				} catch (MissingPunishmentException ex) {}
			}
		}
		return new PunishmentResult();
	}
	
	@Override
	public Set<Punishment> getApplicablePunishments(UUID uuid, String address, PunishmentType type) {
		Subject subject = center.subjects().parseSubject(uuid);
		Subject addressSubj = center.subjects().parseSubject(address);
		Set<Subject> addrSubjs = new HashSet<Subject>();
		if (strict_ip_checking) {
			List<String> ips = (address != null) ? new ArrayList<String>(Arrays.asList(address)) : new ArrayList<String>();
			try {
				ips.addAll(center.resolver().getIps(uuid));
			} catch (MissingCacheException ex) {}
			ips.forEach((addr) -> {
				addrSubjs.add(center.subjects().parseSubject(addr, false));
			});
		} else {
			addrSubjs.addAll(Arrays.asList(addressSubj));
		}
		Set<Punishment> applicable = center.punishments().getActive();
		for (Iterator<Punishment> it = applicable.iterator(); it.hasNext();) {
			Punishment punishment = it.next();
			if (!punishment.type().equals(type) || !punishment.subject().equals(subject)) {
				boolean remove = true;
				if (SubjectType.IP.equals(punishment.subject().getType())) {
					remove = true;
				} else if (strict_ip_checking) {
					for (Subject addrSubj : addrSubjs) {
						if (addrSubj.equals(punishment.subject())) {
							remove = false;
						}
					}
				} else if (!strict_ip_checking) {
					remove = !addressSubj.equals(punishment.subject());
				}
				if (remove) {
					it.remove();
				}
			}
		}
		return applicable;
	}
	
	@Override
	public Punishment getPunishmentById(int id) throws MissingPunishmentException {
		Set<Punishment> punishments = center.punishments().getActive();
		for (Punishment punishment : punishments) {
			if (punishment.id() == id) {
				return punishment;
			}
		}
		throw new MissingPunishmentException(id);
	}
	
	@Override
	public void refreshConfig(boolean first) {
		strict_ip_checking = center.config().getConfigBoolean("enforcement.strict-ip-checking");
	}
	
}
