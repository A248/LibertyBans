package space.arim.bans.api.events.bukkit;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import space.arim.bans.api.Punishment;

public abstract class AbstractPunishEvent extends Event implements Cancellable {
	
	private static final HandlerList HANDLERS = new HandlerList();
	
	protected final Punishment punishment;
	
	private boolean cancel = false;
	
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
	
	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}
	
	@Override
	public void setCancelled(boolean cancel) {
		this.cancel = cancel;
	}
	
	@Override
	public boolean isCancelled() {
		return this.cancel;
	}
	
	public AbstractPunishEvent(final Punishment punishment) {
		super(true);
		this.punishment = punishment;
	}
	
	public Punishment getPunishment() {
		return this.punishment;
	}
	
}
