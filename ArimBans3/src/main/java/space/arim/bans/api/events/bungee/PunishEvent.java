package space.arim.bans.api.events.bungee;

import space.arim.bans.api.Punishment;

public class PunishEvent extends AbstractPunishEvent {

	private final boolean retro;
	
	public PunishEvent(final Punishment punishment) {
		this(punishment, false);
	}
	
	public PunishEvent(final Punishment punishment, boolean retro) {
		super(punishment);
		this.retro = retro;
	}

	/**
	 * Using {@link space.arim.bans.internal.backend.punishment.PunishmentsMaster#addPunishments(Punishment...)},
	 * it is possible for API calls to add punishments whose date is in the past
	 * 
	 * <br><br>Such retrogade punishments are added to punishment history but not to active punishments.
	 * Hence, they aren't enforced, since they're already expired.
	 * 
	 * @return boolean - whether this PunishEvent concerns a retro punishment
	 */
	public boolean isRetrogade() {
		return retro;
	}
	
}
