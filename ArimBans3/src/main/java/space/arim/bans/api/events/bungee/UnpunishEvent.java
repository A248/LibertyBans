package space.arim.bans.api.events.bungee;

import net.md_5.bungee.api.plugin.Cancellable;
import space.arim.bans.api.Punishment;
import space.arim.bans.api.events.UniversalUnpunishEvent;

public class UnpunishEvent extends AbstractBungeeEvent implements UniversalUnpunishEvent, Cancellable {

	private final boolean auto;
	
	private boolean cancel = false;
	
	@Override
	public void setCancelled(boolean cancel) {
		this.cancel = cancel;
	}
	
	@Override
	public boolean isCancelled() {
		return this.cancel;
	}
	
	public UnpunishEvent(final Punishment punishment) {
		this(punishment, false);
	}
	
	public UnpunishEvent(final Punishment punishment, final boolean auto) {
		super(punishment);
		this.auto = auto;
	}

	@Override
	public boolean isAutomatic() {
		return auto;
	}

}
