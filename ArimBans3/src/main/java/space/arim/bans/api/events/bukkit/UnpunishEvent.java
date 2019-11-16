package space.arim.bans.api.events.bukkit;

import space.arim.bans.api.Punishment;

public class UnpunishEvent extends AbstractPunishEvent {

	public UnpunishEvent(final Punishment punishment) {
		super(punishment);
	}
	
}
