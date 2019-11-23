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

import java.util.Objects;

public class Punishment {
	private final PunishmentType type;
	private final Subject subject;
	private final Subject operator;
	private final String reason;
	private final long expiration;
	private final long date;

	public Punishment(PunishmentType type, Subject subject, Subject operator, String reason, long expiration) {
		this(type, subject, operator, reason, expiration, System.currentTimeMillis());
	}
	
	public Punishment(PunishmentType type, Subject subject, Subject operator, String reason, long expiration, long date) {
		Objects.requireNonNull(type, "No field of a Punishment may be null");
		Objects.requireNonNull(subject, "No field of a Punishment may be null");
		Objects.requireNonNull(operator, "No field of a Punishment may be null");
		Objects.requireNonNull(reason, "No field of a Punishment may be null");
		this.type = type;
		this.subject = subject;
		this.operator = operator;
		this.reason = reason;
		this.expiration = expiration;
		this.date = date;
	}

	public PunishmentType type() {
		return type;
	}

	public Subject subject() {
		return subject;
	}

	public Subject operator() {
		return operator;
	}

	public String reason() {
		return reason;
	}

	public long expiration() {
		return expiration;
	}

	public long date() {
		return date;
	}
	
	/**
	 * Compares two punishments
	 * 
	 * @return true if and only if punishments are equal in all their attributes
	 */
	public boolean compare(Punishment punishment) {
		return (type().equals(punishment.type()) && subject().compare(punishment.subject()) && operator().compare(punishment.operator()) && expiration() == punishment.expiration() && date() == punishment.date());
	}
}
