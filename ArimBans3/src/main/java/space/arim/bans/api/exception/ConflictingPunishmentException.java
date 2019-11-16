package space.arim.bans.api.exception;

import space.arim.bans.api.Subject;
import space.arim.bans.api.PunishmentType;

public class ConflictingPunishmentException extends InternalStateException {

	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = -1375684402484585054L;

	public ConflictingPunishmentException(Subject subject, PunishmentType type) {
		super("Subject " + subject + " already has a punishment " + type.deserialise());
	}
	
}
