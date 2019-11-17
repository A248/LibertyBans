package space.arim.bans.api.events.bungee;

import net.md_5.bungee.api.plugin.Event;

import space.arim.bans.api.Punishment;

public abstract class AbstractBungeeEvent extends Event {
	
	protected final Punishment punishment;
	
	public AbstractBungeeEvent(final Punishment punishment) {
		this.punishment = punishment;
	}
	
	public Punishment getPunishment() {
		return this.punishment;
	}
	
}
