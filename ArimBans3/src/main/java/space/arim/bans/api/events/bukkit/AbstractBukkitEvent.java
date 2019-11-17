package space.arim.bans.api.events.bukkit;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import space.arim.bans.api.Punishment;

public abstract class AbstractBukkitEvent extends Event {
	
	private static final HandlerList HANDLERS = new HandlerList();
	
	protected final Punishment punishment;
	
	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
	
	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}
	
	public AbstractBukkitEvent(final Punishment punishment) {
		super(true);
		this.punishment = punishment;
	}
	
	public Punishment getPunishment() {
		return this.punishment;
	}
	
}
