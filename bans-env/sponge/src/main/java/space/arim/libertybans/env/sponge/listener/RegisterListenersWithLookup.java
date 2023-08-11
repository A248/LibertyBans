/*
 * LibertyBans
 * Copyright © 2023 Anand Beh
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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.plugin.PluginContainer;
import space.arim.libertybans.core.env.PlatformListener;

import java.lang.invoke.MethodHandles;

@Singleton
public final class RegisterListenersWithLookup implements RegisterListeners {

	private final PluginContainer plugin;
	private final Game game;

	@Inject
	public RegisterListenersWithLookup(PluginContainer plugin, Game game) {
		this.plugin = plugin;
		this.game = game;
	}

	public static boolean detectIfUsable() {
		try {
			EventManager.class.getMethod("registerListeners", PluginContainer.class, Object.class, MethodHandles.Lookup.class);
			return true;
		} catch (NoSuchMethodException ex) {
			return false;
		}
	}

	@Override
	public void register(PlatformListener listener) {
		game.eventManager().registerListeners(plugin, listener, MethodHandles.lookup());
	}

	@Override
	public void unregister(PlatformListener listener) {
		game.eventManager().unregisterListeners(listener);
	}

}
