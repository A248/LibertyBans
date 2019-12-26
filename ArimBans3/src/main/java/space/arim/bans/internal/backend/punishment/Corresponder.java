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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import space.arim.bans.ArimBans;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentResult;
import space.arim.bans.api.PunishmentType;
import space.arim.bans.api.Subject;
import space.arim.bans.api.Subject.SubjectType;
import space.arim.bans.api.events.PostPunishEvent;
import space.arim.bans.api.events.PostPunishmentChangeReasonEvent;
import space.arim.bans.api.events.PostUnpunishEvent;
import space.arim.bans.api.events.PunishEvent;
import space.arim.bans.api.events.PunishmentChangeReasonEvent;
import space.arim.bans.api.events.UnpunishEvent;
import space.arim.bans.api.exception.MissingCacheException;
import space.arim.bans.api.exception.MissingPunishmentException;

import space.arim.universal.events.UniversalEvents;

public class Corresponder implements CorresponderMaster {

	private final ArimBans center;
	
	private boolean strict_ip_checking;
	
	public Corresponder(ArimBans center) {
		this.center = center;
	}
	
	@Override
	public PunishmentResult getApplicablePunishment(UUID uuid, String address, PunishmentType type) {
		Subject subject = center.subjects().parseSubject(uuid);
		if (center.punishments().hasPunishment(subject, type)) {
			try {
				Punishment punishment = center.punishments().getPunishment(subject, type);
				center.logs().log(Level.CONFIG, "Player:(" + uuid + "," + address + ") has punishment " + type + ".");
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
						center.logs().log(Level.CONFIG, "Player:(" + uuid + "," + address + ") has ip-based punishment " + type + " (strict-ip-checking ENABLED).");
						return new PunishmentResult(subject, punishment, center.formats().formatPunishment(punishment));
					} catch (MissingPunishmentException ex) {}
				}
			}
		} else if (address != null) {
			Subject addrSubj = center.subjects().parseSubject(address, false);
			if (center.punishments().hasPunishment(addrSubj, type)) {
				try {
					Punishment punishment = center.punishments().getPunishment(addrSubj, type);
					center.logs().log(Level.CONFIG, "Player:(" + uuid + "," + address + ") has ip-based punishment " + type + " (strict-ip-checking DISABLED).");
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
			List<String> ips = address != null ? new ArrayList<String>(Arrays.asList(address)) : new ArrayList<String>();
			try {
				ips.addAll(center.resolver().getIps(uuid));
			} catch (MissingCacheException ex) {}
			ips.forEach((addr) -> {
				addrSubjs.add(center.subjects().parseSubject(addr, false));
			});
		} else {
			addrSubjs.add(addressSubj);
		}
		Set<Punishment> applicable = center.punishments().getActiveCopy();
		applicable.removeIf((punishment) -> !punishment.type().equals(type) || !punishment.subject().equals(subject) && (!punishment.subject().getType().equals(SubjectType.IP) || strict_ip_checking && !addrSubjs.contains(punishment.subject()) || !strict_ip_checking && !addressSubj.equals(punishment.subject())));
		center.logs().log(Level.CONFIG, "Player:(" + uuid + "," + address + ") has the following punishments applicable for " + type + ": " + applicable.toString() + " (strict-ip-checking " + (strict_ip_checking ? "ENABLED" : "DISABLED") + " ).");
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
	public boolean callPunishEvent(Punishment punishment, boolean retro) {
		return UniversalEvents.get().fireEvent(new PunishEvent(punishment, retro));
	}
	
	@Override
	public boolean callUnpunishEvent(Punishment punishment, boolean auto) {
		return UniversalEvents.get().fireEvent(new UnpunishEvent(punishment, auto)) || auto;
	}
	
	@Override
	public void callPostPunishEvent(Punishment punishment, boolean retro) {
		UniversalEvents.get().fireEvent(new PostPunishEvent(punishment, retro));
	}
	
	@Override
	public void callPostUnpunishEvent(Punishment punishment, boolean auto) {
		UniversalEvents.get().fireEvent(new PostUnpunishEvent(punishment, auto));
	}
	
	@Override
	public boolean callPunishmentChangeReasonEvent(Punishment punishment, String reason, boolean active) {
		return UniversalEvents.get().fireEvent(new PunishmentChangeReasonEvent(punishment, reason));
	}
	
	@Override
	public void callPostPunishmentChangeReasonEvent(Punishment punishment, String reason, boolean active) {
		UniversalEvents.get().fireEvent(new PostPunishmentChangeReasonEvent(punishment, reason));
	}
	
	@Override
	public void refreshConfig(boolean first) {
		strict_ip_checking = center.config().getConfigBoolean("enforcement.strict-ip-checking");
	}
	
}
