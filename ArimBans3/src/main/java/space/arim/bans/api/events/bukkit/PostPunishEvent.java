package space.arim.bans.api.events.bukkit;

import space.arim.bans.api.Punishment;
import space.arim.bans.api.events.UniversalPunishEvent;

public class PostPunishEvent extends AbstractBukkitEvent implements UniversalPunishEvent {

	private final boolean retro;
	
	public PostPunishEvent(final Punishment punishment) {
		this(punishment, false);
	}
	
	public PostPunishEvent(final Punishment punishment, boolean retro) {
		super(punishment);
		this.retro = retro;
	}

	@Override
	public boolean isRetrogade() {
		return retro;
	}

}
