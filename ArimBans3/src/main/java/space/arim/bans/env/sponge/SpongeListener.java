/*
 * ArimBans3, a punishment plugin for minecraft servers
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * ArimBans3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ArimBans3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ArimBans3. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU General Public License.
 */
package space.arim.bans.env.sponge;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;

public class SpongeListener {

	private final SpongeEnv environment;
	
	public SpongeListener(SpongeEnv environment) {
		this.environment = environment;
	}
	
	@Listener(order = Order.POST)
	public void cacheData(ClientConnectionEvent.Auth evt) {
		environment.enforcer().updateCache(evt);
	}
	
	@Listener(order = Order.LAST)
	public void enforceBansHighest(ClientConnectionEvent.Auth evt) {
		environment.enforcer().enforceBans(evt, Order.LAST);
	}
	
	@Listener(order = Order.LATE)
	public void enforceBansHigh(ClientConnectionEvent.Auth evt) {
		environment.enforcer().enforceBans(evt, Order.LATE);
	}
	
	@Listener(order = Order.DEFAULT)
	public void enforceBansNormal(ClientConnectionEvent.Auth evt) {
		environment.enforcer().enforceBans(evt, Order.DEFAULT);
	}
	
	@Listener(order = Order.EARLY)
	public void enforceBansLow(ClientConnectionEvent.Auth evt) {
		environment.enforcer().enforceBans(evt, Order.EARLY);
	}
	
	@Listener(order = Order.FIRST)
	public void enforceBansLowest(ClientConnectionEvent.Auth evt) {
		environment.enforcer().enforceBans(evt, Order.FIRST);
	}
	
	@Listener(order = Order.LAST)
	public void enforceMutesHighest(MessageChannelEvent.Chat evt, @First Player player) {
		environment.enforcer().enforceMutes(evt, Order.LAST, player);
	}
	
	@Listener(order = Order.LATE)
	public void enforceMutesHigh(MessageChannelEvent.Chat evt, @First Player player) {
		environment.enforcer().enforceMutes(evt, Order.LATE, player);
	}
	
	@Listener(order = Order.DEFAULT)
	public void enforceMutesNormal(MessageChannelEvent.Chat evt, @First Player player) {
		environment.enforcer().enforceMutes(evt, Order.DEFAULT, player);
	}
	
	@Listener(order = Order.EARLY)
	public void enforceMutesLow(MessageChannelEvent.Chat evt, @First Player player) {
		environment.enforcer().enforceMutes(evt, Order.EARLY, player);
	}
	
	@Listener(order = Order.FIRST)
	public void enforceMutesLowest(MessageChannelEvent.Chat evt, @First Player player) {
		environment.enforcer().enforceMutes(evt, Order.FIRST, player);
	}
	
	@Listener(order = Order.LAST)
	public void enforceMutesHighest(SendCommandEvent evt, @First Player player) {
		environment.enforcer().enforceMutes(evt, Order.LAST, player);
	}
	
	@Listener(order = Order.LATE)
	public void enforceMutesHigh(SendCommandEvent evt, @First Player player) {
		environment.enforcer().enforceMutes(evt, Order.LATE, player);
	}
	
	@Listener(order = Order.DEFAULT)
	public void enforceMutesNormal(SendCommandEvent evt, @First Player player) {
		environment.enforcer().enforceMutes(evt, Order.DEFAULT, player);
	}
	
	@Listener(order = Order.EARLY)
	public void enforceMutesLow(SendCommandEvent evt, @First Player player) {
		environment.enforcer().enforceMutes(evt, Order.EARLY, player);
	}
	
	@Listener(order = Order.FIRST)
	public void enforceMutesLowest(SendCommandEvent evt, @First Player player) {
		environment.enforcer().enforceMutes(evt, Order.FIRST, player);
	}
	
}
