/* 
 * ArimBansLib, an API for ArimBans
 * Copyright © 2019 Anand Beh <https://www.arim.space>
 * 
 * ArimBansLib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBansLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBansLib. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
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
