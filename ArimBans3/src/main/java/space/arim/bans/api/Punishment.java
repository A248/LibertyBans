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
		if (this.date == punishment.date()) {
			if (this.type.equals(punishment.type()) && this.subject.compare(punishment.subject()) && this.operator.compare(punishment.operator()) && this.expiration == punishment.expiration()) {
				return true;
			}
		}
		return false;
	}
}
