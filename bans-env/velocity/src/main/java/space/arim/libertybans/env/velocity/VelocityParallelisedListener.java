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
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;

abstract class VelocityParallelisedListener<E extends ResultedEvent<?>, R> extends ParallelisedListener<E, R> {

	private final PluginContainer plugin;
	private final ProxyServer server;

	private final EarlyHandler earlyHandler = new EarlyHandler();
	private final LateHandler lateHandler = new LateHandler();
	
	VelocityParallelisedListener(PluginContainer plugin, ProxyServer server) {
		this.plugin = plugin;
		this.server = server;
	}

	@Override
	public final void register() {
		Class<E> eventClass = getEventClass();
		EventManager eventManager = server.getEventManager();
		eventManager.register(plugin, eventClass, PostOrder.EARLY, earlyHandler);
		eventManager.register(plugin, eventClass, PostOrder.LATE, lateHandler);
	}

	abstract Class<E> getEventClass();
	
	@Override
	public void unregister() {
		EventManager eventManager = server.getEventManager();
		eventManager.unregister(plugin, earlyHandler);
		eventManager.unregister(plugin, lateHandler);
	}

	@Override
	protected final boolean isAllowed(E event) {
		return event.getResult().isAllowed();
	}

	/** Can be overridden to skip events */
	protected boolean filter(E event) {
		return true;
	}

	protected abstract CentralisedFuture<R> beginComputation(E event);

	protected abstract void executeNonNullResult(E event, R result);

	private class EarlyHandler implements EventHandler<E> {

		@Override
		public void execute(E event) {
			if (!filter(event)) {
				return;
			}
			if (!event.getResult().isAllowed()) {
				debugPrematurelyDenied(event);
				return;
			}
			CentralisedFuture<R> future = beginComputation(event);
			begin(event, future);
		}

	}

	private class LateHandler implements EventHandler<E> {

		@Override
		public void execute(E event) {
			if (!filter(event)) {
				return;
			}
			R result = withdraw(event);
			if (result == null) {
				debugResultPermitted(event);
				return;
			}
			executeNonNullResult(event, result);
		}
		
	}
	
}
