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

package space.arim.libertybans.env.sponge.listener;

import io.leangen.geantyref.TypeToken;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.EventListenerRegistration;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.plugin.PluginContainer;
import space.arim.libertybans.core.env.PlatformListener;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
public final class RegisterListenersByMethodScan implements RegisterListeners {

	private final PluginContainer plugin;
	private final Game game;

	private final ConcurrentMap<PlatformListener, EventListener<?>[]> registrations = new ConcurrentHashMap<>();

	@Inject
	public RegisterListenersByMethodScan(PluginContainer plugin, Game game) {
		this.plugin = plugin;
		this.game = game;
	}

	@Override
	public void register(PlatformListener listener) {
		if (registrations.containsKey(listener)) {
			throw new IllegalStateException("Listener already registered: " + listener);
		}
		List<EventListenerRegistration<?>> registeredMethods = collectMethods(listener);
		EventManager eventManager = game.eventManager();
		registrations.compute(listener, (l, existingMethods) -> {
			if (existingMethods != null) {
				throw new IllegalStateException("Listener already registered: " + l);
			}
			registeredMethods.forEach(eventManager::registerListener);
			EventListener<?>[] eventListeners = new EventListener[registeredMethods.size()];
			for (int n = 0; n < eventListeners.length; n++) {
				eventListeners[n] = registeredMethods.get(n).listener();
			}
			return eventListeners;
		});
	}

	private List<EventListenerRegistration<?>> collectMethods(Object listener) {
		List<EventListenerRegistration<?>> registeredMethods = new ArrayList<>();
		for (Method method : listener.getClass().getDeclaredMethods()) {
			if (method.getParameterCount() != 1 || method.getReturnType() != void.class) {
				continue;
			}
			Listener listenerAnnotation = method.getAnnotation(Listener.class);
			if (listenerAnnotation == null) {
				continue;
			}
			Class<?> eventParameter = method.getParameterTypes()[0];
			if (!Event.class.isAssignableFrom(eventParameter)) {
				continue;
			}
			registeredMethods.add(createRegistration(
					eventParameter.asSubclass(Event.class),
					listener,
					method,
					listenerAnnotation
			));
		}
		return registeredMethods;
	}

	private <E extends Event> EventListenerRegistration<E> createRegistration(
			Class<E> eventClass, Object listener, Method method, Listener listenerAnnotation) {
		MethodHandle methodHandle;
		try {
			methodHandle = MethodHandles.lookup().unreflect(method);
		} catch (IllegalAccessException ex) {
			throw new RuntimeException("Unable to create listener registration", ex);
		}
		return game.factoryProvider().provide(EventListenerRegistration.Factory.class)
				.builder(TypeToken.get(eventClass))
				.beforeModifications(listenerAnnotation.beforeModifications())
				.order(listenerAnnotation.order())
				.plugin(plugin)
				.listener((event) -> {
					try {
						methodHandle.invoke(listener, event);
					} catch (Exception ex) {
						throw ex;
					} catch (Throwable ex) {
						throw new RuntimeException(ex);
					}
				})
				.build();
	}

	@Override
	public void unregister(PlatformListener listener) {
		EventManager eventManager = game.eventManager();
		for (EventListener<?> eventListener : registrations.remove(listener)) {
			eventManager.unregisterListeners(eventListener);
		}
	}

}
