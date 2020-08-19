/* 
 * LibertyBans-env-velocity
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-env-velocity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-env-velocity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-env-velocity. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.env.velocity;

import space.arim.omnibus.util.concurrent.CentralisedFuture;

import space.arim.libertybans.core.env.ParallelisedListener;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.plugin.PluginContainer;

abstract class VelocityParallelisedListener<E, R> extends ParallelisedListener<E, R>{

	final VelocityEnv env;
	
	private final EarlyHandler earlyHandler = new EarlyHandler();
	private final LateHandler lateHandler = new LateHandler();
	
	VelocityParallelisedListener(VelocityEnv env) {
		this.env = env;
	}
	
	void register(Class<E> evtClass) {
		PluginContainer plugin = env.getPlugin();
		EventManager evtManager = env.getServer().getEventManager();
		evtManager.register(plugin, evtClass, PostOrder.EARLY, earlyHandler);
		evtManager.register(plugin, evtClass, PostOrder.LATE, lateHandler);
	}
	
	@Override
	public void unregister() {
		PluginContainer plugin = env.getPlugin();
		EventManager evtManager = env.getServer().getEventManager();
		evtManager.unregister(plugin, earlyHandler);
		evtManager.unregister(plugin, lateHandler);
	}
	
	protected abstract CentralisedFuture<R> beginFor(E event);
	
	protected abstract void withdrawFor(E event);
	
	private class EarlyHandler implements EventHandler<E> {

		@Override
		public void execute(E event) {
			CentralisedFuture<R> future = beginFor(event);
			if (future != null) {
				begin(event, future);
			}
		}
		
	}
	
	private class LateHandler implements EventHandler<E> {

		@Override
		public void execute(E event) {
			withdrawFor(event);
		}
		
	}
	
}
