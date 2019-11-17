package space.arim.bans.api.events;

public interface UniversalPunishEvent {
	/**
	 * Using {@link space.arim.bans.internal.backend.punishment.PunishmentsMaster#addPunishments(Punishment...)},
	 * it is possible for API calls to add punishments whose date is in the past
	 * 
	 * <br><br>Such retrogade punishments are added to punishment history but not to active punishments.
	 * Hence, they aren't enforced, since they're already expired.
	 * 
	 * @return boolean - whether this PunishEvent concerns a retro punishment
	 */
	boolean isRetrogade();
}
