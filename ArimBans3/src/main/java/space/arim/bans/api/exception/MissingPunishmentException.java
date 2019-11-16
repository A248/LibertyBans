package space.arim.bans.api.exception;

import space.arim.bans.api.Subject;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.PunishmentType;

public class MissingPunishmentException extends InternalStateException {

	private static final long serialVersionUID = -3749852643537426416L;

	public MissingPunishmentException(Subject subject, PunishmentType type) {
		super("Subject " + subject + " does not have a corresponding punishment " + type.deserialise());
	}

	public MissingPunishmentException(Punishment fakePunishment) {
		super("Punishment with these details does not exist: " + fakePunishment.type() + "/" + fakePunishment.subject() + "/" + fakePunishment.operator() + "/" + fakePunishment.expiration() + "/" + fakePunishment.date());
	}
}
