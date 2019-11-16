package space.arim.bans.api.events.bungee;

import net.md_5.bungee.api.plugin.Cancellable;
import net.md_5.bungee.api.plugin.Event;

import space.arim.bans.api.Punishment;

public abstract class AbstractPunishEvent extends Event implements Cancellable {
	
	protected final Punishment punishment;
	
	private boolean cancel = false;
	
	@Override
	public void setCancelled(boolean cancel) {
		this.cancel = cancel;
	}
	
	@Override
	public boolean isCancelled() {
		return this.cancel;
	}
	
	public AbstractPunishEvent(final Punishment punishment) {
		this.punishment = punishment;
	}
	
	public Punishment getPunishment() {
		return this.punishment;
	}
	
}
