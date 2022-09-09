/*
 * LibertyBans
 * Copyright © 2022 Anand Beh
 *
 * LibertyBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * LibertyBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */

package space.arim.libertybans.env.velocity;

import com.velocitypowered.api.event.AwaitingEventExecutor;
import com.velocitypowered.api.event.Continuation;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.arim.libertybans.core.env.PlatformListener;
import space.arim.omnibus.util.ThisClass;
import space.arim.omnibus.util.concurrent.CentralisedFuture;
import space.arim.omnibus.util.concurrent.impl.IndifferentCentralisedFuture;

abstract class VelocityAsyncListener<E extends ResultedEvent<?>, R> implements PlatformListener {

	private final PluginContainer plugin;
	private final ProxyServer server;

	// Visible for testing
	final AsyncHandler handler = new AsyncHandler();

	private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());

	VelocityAsyncListener(PluginContainer plugin, ProxyServer server) {
		this.plugin = plugin;
		this.server = server;
	}

	@Override
	public final void register() {
		Class<E> eventClass = getEventClass();
		EventManager eventManager = server.getEventManager();
		eventManager.register(plugin, eventClass, PostOrder.EARLY, handler);
	}

	abstract Class<E> getEventClass();
	
	@Override
	public void unregister() {
		EventManager eventManager = server.getEventManager();
		eventManager.unregister(plugin, handler);
	}

	/** Can be overridden to skip events */
	protected boolean skipEvent(E event) {
		return false;
	}

	protected abstract CentralisedFuture<R> beginComputation(E event);

	protected abstract void executeNonNullResult(E event, R result);

	// Visible for testing
	class AsyncHandler implements AwaitingEventExecutor<E> {

		@Override
		public EventTask executeAsync(E event) {
			if (skipEvent(event)) {
				return null;
			}
			if (!event.getResult().isAllowed()) {
				logger.debug("Event {} is already blocked", event);
				return null;
			}
			CentralisedFuture<R> future = beginComputation(event);
			return EventTask.resumeWhenComplete(future.thenAccept((result) -> {
				if (result == null) {
					logger.trace("Event {} will be permitted", event);
					return;
				}
				executeNonNullResult(event, result);
			}));
		}

		void executeAndWait(E event) {
			EventTask eventTask = executeAsync(event);
			if (eventTask == null) {
				return;
			}
			CentralisedFuture<Void> future = new IndifferentCentralisedFuture<>();
			eventTask.execute(new Continuation() {
				@Override
				public void resume() {
					future.complete(null);
				}

				@Override
				public void resumeWithException(Throwable exception) {
					future.completeExceptionally(exception);
				}
			});
			future.join();
		}
	}
	
}
